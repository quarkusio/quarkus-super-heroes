import groovy.json.*

def inputFile = new File(args[0])
assert inputFile.exists()

def outputFile = new File(args[1])

def json = new JsonSlurper().parse inputFile
assert json instanceof List

println "There are ${json.size()} entries in ${inputFile}"

json.each { supe ->
  supe.name = supe.name?.replaceAll("'", "''")?.minus("#")
  supe.otherName = supe.otherName?.replaceAll("'", "''")?.minus("#")
  supe.picture = supe.picture?.replaceAll("'", "''")?.minus("#")
  supe.powers = supe.powers?.join(", ")?.minus("#")
}

def builder = new JsonBuilder(json)

println "Writing output to ${outputFile}"
outputFile.text = new JsonBuilder(json).toPrettyString()
