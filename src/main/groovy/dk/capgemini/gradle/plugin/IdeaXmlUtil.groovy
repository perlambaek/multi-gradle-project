package dk.capgemini.gradle.plugin


class IdeaXmlUtil {

    static Node replaceJarWithModuleDependency(Node root, String jarSearch, String module) {
        Node entry = root.component.orderEntry.find {
            it.library.CLASSES.root.@url[0]?.contains(jarSearch)
        }
        //<orderEntry type="module" module-name="patient" />
        if (entry != null) {
            Node newNode = entry.replaceNode {
                orderEntry(type: 'module', 'module-name': module, exported: '')
            }
        }
        return root
    }

    static def removeJarDependency(Node root, String jarSearch) {
        root.component.orderEntry.findAll {
            it.library.CLASSES.root.@url[0]?.contains(jarSearch)
        }.each {
            Node node ->
                node.parent().remove(node)
        }
    }
}
