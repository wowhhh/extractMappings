import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

//包名+类名+返回值类型+方法名+参数
// <包名.类名: 返回值 方法名(参数...)>
public class ExtractMappings {
    private static API api = new API();
    private static WRFile wrFile = new WRFile();
    public static void init() throws IOException {

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JarTypeSolver(config.pathToJar)); // 引入android.jar sdk
//        combinedTypeSolver.add(new JavaParserTypeSolver(config.sourcePath));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

    }
    /**
     * 获取包名
     * @param file
     * @return
     */
    public static String getPackageName(File file)
    {
//        D:\Android\AndroidPlatform\source\sources-25_r01\src
        String filePath = file.toString();
        String[] firstSplit = filePath.split("\\\\");
        String packageName = "";
        for(int i=6;i<firstSplit.length-1;i++)
        {
            packageName+=firstSplit[i]+".";
        }
        api.setPackageName(packageName);
        return  null;
    }
    public static String getClassFullName(String path,String className)
    {
        if(path == null) return className+"没找到！！！";
        String filePath = path;
        String[] firstSplit = filePath.split("\\\\");
        String fullName = "";
        for(int i=6;i<firstSplit.length-1;i++)
        {
            fullName+=firstSplit[i]+".";
        }
        fullName+=className;
        return  fullName;
    }
    /**
     *
     * @param javaFile
     * @throws IOException
     */
    public static void inspectJavaFile(File javaFile) throws IOException {
        init();
        CompilationUnit cu = null;
        FileInputStream in = new FileInputStream(javaFile);
        try {
//            cu = new JavaParser().parse(in);
            cu = StaticJavaParser.parse(in);
            ClassVisitor classVisitor = new ClassVisitor();
            classVisitor.visit(cu,null);
            getPackageName(javaFile);
            //a
            MethodAnnotationVisitor annotationVisitor = new MethodAnnotationVisitor();
            annotationVisitor.visit(cu, null);
            //d
            MethodDocVisitor docVisitor = new MethodDocVisitor();
            docVisitor.visit(cu,null);
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            in.close();
        }


    }

    private static class MethodDocVisitor extends VoidVisitorAdapter
    {
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            boolean needPermission = false;
            String permission = new String();
            String returnType = new String();
            String methodName = new String();
            ArrayList<String> paras = new ArrayList<String>();
            String filePath = null;

            //JavaDoc
            if(n.getJavadoc()!=null)
            {

                if(n.getJavadoc().toString().contains("Requires Permission"))
                {
                    Javadoc j = n.getJavadoc().get();
                    for(JavadocDescriptionElement element : j.getDescription().getElements())
                    {
                        if(element.toString().contains(".permission#")) {
                            System.out.println(element.toString());
                            String[] firstSplit =  element.toString().split("content=' ");
                            String[] secondSplit = firstSplit[1].split("'}");
                            permission += secondSplit[0]+" ";
                        }
                    }
                    System.out.println(permission);
                    needPermission = true;
                    filePath = "out\\"+config.sourceVersion+"docs.txt";
                }

            }
            if(needPermission) {
                System.out.println("**************Begin***********");
                for (Parameter p : n.getParameters())//参数类型
                {
                    /**
                     * p.getName() 形参名字
                     */
                    try {
                        paras.add(p.getType().resolve().describe());
                    }
                    catch (Exception e)
                    {
//                        paras.add(p.getType().toString()+"!!UnsolvedSymbolException!!");
                        //emmmm 在source中查找此类的限定名
                        String test = getClassFullName(getFullQualityName(config.sourcePath,p.getType().toString()),
                                p.getType().toString());
                        paras.add(test);
//                        getFullQualityName(config.sourcePath,p.getType().toString());
                    }
                }
                try {
                    returnType = n.getType().resolve().describe();
                }
                catch (Exception e)
                {
//                    paras.add(n.getType().toString()+"!!UnsolvedSymbolException!!");
                    String test = getClassFullName(getFullQualityName(config.sourcePath,n.getType().toString()),
                            n.getType().toString());
                    paras.add(test);
                }
                methodName = n.getNameAsString();

                System.out.println("##########Out##############");
                String single = "";
                single += api.getPackageName()+api.getClassName()+"."+methodName+"(";
                for(int i=0;i<paras.size();i++)
                {
                    if(i!=0){single+=","+paras.get(i);}
                    else{single+= paras.get(i);}
                }
                single+=")"+returnType;
                single +=" :: " + permission;
//                System.out.println(single);
                wrFile.saveContent(single,filePath);
                System.out.println("##########Out End##############");

                System.out.println("**************over***********");
            }

        }
    }
    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodAnnotationVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            boolean needPermission = false;
            String permission = new String();
            String returnType = new String();
            String methodName = new String();
            ArrayList<String> paras = new ArrayList<String>();
            String filePath = null;
            //Annotations
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    System.out.println(annotation.getName());
                    if(annotation.getName().toString().contains("RequiresPermission"))
                    {
                        needPermission = true;
                        permission = annotation.toString(); //@RequiresPermission(anyOf = { ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION })
                        filePath = "out\\"+config.sourceVersion+"annotationsMappings.txt";
                    }

                 }
                //处理annotation得到的权限
                if(needPermission) {
                    permission = extractPermissionByAnnotation(permission);
                }
            }
            if(needPermission) {
                System.out.println("**************Begin***********");
                for (Parameter p : n.getParameters())//参数类型
                {
                    /**
                     * p.getName() 形参名字
                     */
                    try {
                        paras.add(p.getType().resolve().describe());
                    }
                    catch (Exception e)
                    {
//                        paras.add(p.getType().toString()+"!!UnsolvedSymbolException!!");
                        //emmmm 在source中查找此类的限定名
                        String test = getClassFullName(getFullQualityName(config.sourcePath,p.getType().toString()),
                                p.getType().toString());
                        paras.add(test);
//                        getFullQualityName(config.sourcePath,p.getType().toString());
                    }
                }
                try {
                    returnType = n.getType().resolve().describe();
                }
                catch (Exception e)
                {
//                    paras.add(n.getType().toString()+"!!UnsolvedSymbolException!!");
                    String test = getClassFullName(getFullQualityName(config.sourcePath,n.getType().toString()),
                            n.getType().toString());
                    paras.add(test);
                }
                methodName = n.getNameAsString();

                System.out.println("##########Out##############");
                String single = "";
                single += api.getPackageName()+api.getClassName()+"."+methodName+"(";
                for(int i=0;i<paras.size();i++)
                {
                    if(i!=0){single+=","+paras.get(i);}
                    else{single+= paras.get(i);}
                }
                single+=")"+returnType;
                single +=" :: " + permission;
//                System.out.println(single);
                wrFile.saveContent(single,filePath);
                System.out.println("##########Out End##############");

                System.out.println("**************over***********");
            }
        }

    }
    /**
     * @RequiresPermission(USE_FINGERPRINT)
     * @RequiresPermission(android.Manifest.permission.CALL_PHONE)
     * @RequiresPermission(anyOf = { ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION })
     * @RequiresPermission(allOf = { Manifest.permission.INTERACT_ACROSS_USERS_FULL, Manifest.permission.MANAGE_USERS })
     * android.allOf = { Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.UPDATE_DEVICE_STATS }
     */
    private static   String extractPermissionByAnnotation(String permission) {
        String returnPermission = "";
        try {
            // all of
            if (permission.contains("anyOf") || permission.contains("allOf")) {
                String[] first = null;
                if(permission.contains("anyOf")){first = permission.split("anyOf = \\{ ");}
                if(permission.contains("allOf")){first = permission.split("allOf = \\{ ");}
                String[] second = first[1].split(" }\\)");
                String[] permissions = second[0].split(", ");

                for (String s : permissions) {
                    if (s.contains("Manifest.permission")) {
                        s = "android." + s;
                    } else if (s.contains("android.Manifest.permission")) {
                    } else {
                        s = "android.Manifest.permission." + s;
                    }

                    if (returnPermission == "" && permission.contains("anyOf")) {
                        returnPermission +=  "AnyOf ==>"+s;
                    }
                    else if(returnPermission == "" && permission.contains("allOf"))
                    {
                        returnPermission +=  "AllOf ==>"+s;
                    }
                    else {
                        returnPermission += ", " + s;
                    }
                }
            } else if (permission.contains("RequiresPermission")){
                String[] first = permission.split("\\(");
                String[] second = first[1].split("\\)");
                returnPermission = second[0];
                if (returnPermission.contains("Manifest.permission")) {
                    returnPermission = "android." + returnPermission;
                } else if (returnPermission.contains("android.Manifest.permission.")) {
                } else {
                    returnPermission = "android.Manifest.permission." + returnPermission;
                }
            }
            else
            {
                System.out.println(permission);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return returnPermission;
    }

    private static String getFullQualityName(String filePath,String className) {
        //在文件夹中查找补全限定名
        File file = new File(filePath);
        File [] files = file.listFiles();

        for(File a:files){
            if(a.isDirectory()){//如果文件夹下有子文件夹，获取子文件夹下的所有文件全路径。
                getFullQualityName(a.getAbsolutePath()+"\\",className);
            }
            else if(a.isFile()) //为文件
            {
                if(a.toString().contains(className))
                {
                    return a.toString();
                }
            }
        }
        return null;
    }

    private static class ClassVisitor extends VoidVisitorAdapter
    {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            api.setClassName(n.getNameAsString());

            System.out.println(n.getNameAsString());

        }
    }
}

