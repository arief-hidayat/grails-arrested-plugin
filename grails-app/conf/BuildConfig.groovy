grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile 'org.apache.shiro:shiro-core:1.2.0'
    }

    plugins {
        runtime ":cors:1.1.4"
        build ':release:2.2.1', ':rest-client-builder:1.0.3', {
            export = false
        }
    }
}