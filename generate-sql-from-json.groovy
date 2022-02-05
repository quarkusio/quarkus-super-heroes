import groovy.json.JsonSlurper

def inputFile = new File(args[0])
assert inputFile.exists()

def outputFile = new File(args[1])

def tableName = args[2]
assert tableName != null

def json = new JsonSlurper().parse inputFile
assert json instanceof List

println "There are ${json.size()} entries in ${inputFile}"

def output = new StringBuilder()

json.each { supe ->
	output << "INSERT INTO ${tableName}(id, name, otherName, picture, powers, level)\n"
	output << "VALUES (nextval('hibernate_sequence'), '${supe.name?.replaceAll("'", "''")?.minus("#")}', '${supe.otherName?.replaceAll("'", "''")?.minus("#")}', '${supe.picture?.replaceAll("'", "''")?.minus("#")}', '${supe.powers?.join(", ")?.minus("#")}', ${supe.level});\n"
}

println "Writing output to ${outputFile}"
outputFile.text = output.toString()
