import grails.util.GrailsNameUtils
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.core.io.ResourceLoader


includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsCreateArtifacts")


installTemplate = { String artefactName, String artefactPath, String templatePath ->
    installTemplateEx(artefactName, artefactPath, templatePath, artefactName, null)
}

installTemplateEx = { String artefactName, String artefactPath, String templatePath, String templateName, Closure c ->
    // Copy over the standard auth controller.
    def artefactFile = "${basedir}/${artefactPath}/${artefactName}"

    if (new File(artefactFile).exists()) {
        ant.input(
                addProperty: "${args}.${artefactName}.overwrite",
                message: "${artefactName} already exists. Overwrite? [y/n]")

        if (ant.antProject.properties."${args}.${artefactName}.overwrite" == "n") {
            return
        }
    }

    // Copy the template file to the 'grails-app/controllers' directory.
    templateFile = "${arrestedPluginDir}/src/templates/${templatePath}/${templateName}"
    println(templateFile)
    if (!new File(templateFile).exists()) {
        ant.echo("[Arrested plugin] Error: src/templates/${templatePath}/${templateName} does not exist!")
        return
    }

    ant.copy(file: templateFile, tofile: artefactFile, overwrite: true)

    // Perform any custom processing that may be required.
    if (c) {
        c.delegate = [ artefactFile: artefactFile ]
        c.call()
    }
    event("CreatedFile", [artefactFile])
}

installTemplateView = { domainClass, String artefactName, String artefactPath, String templatePath, String templateName, Closure c ->
    // Copy over the standard auth controller.
     Template renderEditorTemplate;
     SimpleTemplateEngine engine = new SimpleTemplateEngine();
     GrailsPluginManager pluginManager
     ResourceLoader resourceLoader
    def cp


    def artefactFile = "${basedir}/${artefactPath}/${artefactName}"

    if (new File(artefactFile).exists()) {
        ant.input(
                addProperty: "${args}.${artefactName}.overwrite",
                message: "${artefactName} already exists. Overwrite? [y/n]")

        if (ant.antProject.properties."${args}.${artefactName}.overwrite" == "n") {
            return
        }
    }
    // Copy the template file to the 'grails-app/controllers' directory.
    templateFile = "${arrestedPluginDir}/src/templates/${templatePath}/${templateName}"
    if (!new File(templateFile).exists()) {
        ant.echo("[Arrested plugin] Error: src/templates/${templatePath}/${templateName} does not exist!")
        return
    }

    renderEditorTemplate = engine.createTemplate(new File(templateFile))

    boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate') || pluginManager?.hasGrailsPlugin('hibernate4')
    if (hasHibernate) {
        cp = domainClass.constrainedProperties[artefactName]
    }

    def binding = [
            domainTitle:domainClass.getFullName(),
            domainInstance:domainClass.getPropertyName(),
            domainClass:domainClass.getProperties(),
            pluginManager:pluginManager,
            cp:cp]


    ant.copy(file: templateFile, tofile: artefactFile, overwrite: true)

    File file = new File(artefactFile);
    BufferedWriter output = new BufferedWriter(new FileWriter(file));
    output.write(renderEditorTemplate.make(binding).toString());
    output.close();




    // Perform any custom processing that may be required.
    if (c) {
        c.delegate = [ artefactFile: artefactFile ]
        c.call()
    }
    event("CreatedFile", [artefactFile])
}

target(createController: "Creates a standard controller") {
    def (pkg, prefix) = parsePrefix()
    // Copy over the standard filters class.
    def className = prefix+"Controller"
    installTemplateEx("${className}.groovy", "grails-app/controllers${packageToPath(pkg)}", "controllers", "Controller.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
            ant.replacefilter(token: '@controller.name@', value: className)
            ant.replacefilter(token: '@class.name@', value: prefix.toUpperCase())
            ant.replacefilter(token: '@class.instance@', value: prefix)
        }
    }
}
target(createJSController: "Creates a standard angular controller") {
    def (pkg, prefix) = parsePrefix()
    // Copy over the standard filters class.
    def className = prefix+"Ctrl"
    installTemplateEx("${className}.js", "web-app/js/${packageToPath(pkg)}", "views/controllers", "Controller.js") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: '@controller.name@', value: className)
            ant.replacefilter(token: '@class.name@', value: prefix.toUpperCase())
            ant.replacefilter(token: '@class.instance@', value: prefix)
        }
    }
}
target(createToken: "Create a token class") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("ArrestedToken.groovy", "grails-app/domain${packageToPath(pkg)}", "classes", "ArrestedToken.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(createUser: "Create a user class") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("ArrestedUser.groovy", "grails-app/domain${packageToPath(pkg)}", "classes", "ArrestedUser.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(createViewController: "Creates view") {
    depends(loadApp)
    def (pkg, prefix) = parsePrefix()

    def domainFile = "${basedir}/grails-app/domain/${packageToPath(pkg)}"+prefix+".groovy"
    if (new File(domainFile).exists()) {

        def domainClasses = grailsApp.domainClasses
        domainClasses.each {
            domainClass ->
                if(domainClass.getFullName()== prefix){
//                  *********** LIST
                    def className = domainClass.getPropertyName()
                    installTemplateView(domainClass,"list.gsp", "grails-app/views/${packageToPath(pkg)}${className}", "views/view", "list.gsp") {}
                    installTemplateView(domainClass,"edit.gsp", "grails-app/views/${packageToPath(pkg)}${className}", "views/view", "edit.gsp") {}
              }
        }
    }
}
target(createUserController: "Create a user class") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("ArrestedUserController.groovy", "grails-app/controllers${packageToPath(pkg)}", "controllers", "ArrestedUserController.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(createAuth: "Create a authentication controller") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("AuthController.groovy", "grails-app/controllers${packageToPath(pkg)}", "controllers", "AuthController.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(createFilter: "Create a security filter") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("SecurityFilters.groovy", "grails-app/conf${packageToPath(pkg)}", "configuration", "SecurityFilters.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(updateUrl: "Updating the Url Mappings") {
    def (pkg, prefix) = parsePrefix()
    installTemplateEx("UrlMappings.groovy", "grails-app/conf${packageToPath(pkg)}", "configuration", "UrlMappings.groovy") {
        ant.replace(file: artefactFile) {
            ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
        }
    }
}
target(createAll: "Quick Start"){
    depends(loadApp)
    def (pkg, prefix) = parsePrefix()

        def domainClasses = grailsApp.domainClasses
        domainClasses.each {
            domainClass ->
                def className = domainClass.getPropertyName()
                installTemplateView(domainClass,"list.gsp", "grails-app/views/${packageToPath(pkg)}${className}", "views/view", "list.gsp") {}
                installTemplateView(domainClass,"edit.gsp", "grails-app/views/${packageToPath(pkg)}${className}", "views/view", "edit.gsp") {}

                className = className+"Controller"
                installTemplateEx("${className}.groovy", "grails-app/controllers${packageToPath(pkg)}", "controllers", "Controller.groovy") {
                    ant.replace(file: artefactFile) {
                        ant.replacefilter(token: "@package.line@", value: (pkg ? "package ${pkg}\n\n" : ""))
                        ant.replacefilter(token: '@controller.name@', value: className)
                        ant.replacefilter(token: '@class.name@', value: prefix.toUpperCase())
                        ant.replacefilter(token: '@class.instance@', value: prefix)
                    }
                }
                className = domainClass.getPropertyName()+"Ctrl"
                installTemplateEx("${className}.js", "web-app/js/${packageToPath(pkg)}", "views/controllers", "Controller.js") {
                    ant.replace(file: artefactFile) {
                        ant.replacefilter(token: '@controller.name@', value: className)
                        ant.replacefilter(token: '@class.name@', value: prefix.toUpperCase())
                        ant.replacefilter(token: '@class.instance@', value: prefix)
                    }
                }
        }
}

private parsePrefix() {
    def prefix = "Arrested"
    def pkg = ""
    if (argsMap["name"] != null) {
        def givenValue = argsMap["name"].split(/\./, -1)
        prefix = givenValue[-1]
        pkg = givenValue.size() > 1 ? givenValue[0..-2].join('.') : ""
    }
    return [ pkg, prefix ]
}
private packageToPath(String pkg) {
    return pkg ? '/' + pkg.replace('.' as char, '/' as char) : ''
}