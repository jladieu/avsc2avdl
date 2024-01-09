import io.github.wjur.avsc2avdl.api.Avsc2AvdlFacade
import java.io.File

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: avsc2avdl [input.avsc] [output.avdl]")
        return
    }

    val inputAvscFile = File(args[0])
    val outputAvdlFile = File(args[1])

    if (!inputAvscFile.exists()) {
        println("Input file does not exist: ${inputAvscFile.absolutePath}")
        return
    }

    val avdlContent = Avsc2AvdlFacade.INSTANCE.convert(args[0])

    outputAvdlFile.writeText(avdlContent)

    println("Conversion successful. AVDL written to: ${outputAvdlFile.absolutePath}")
}
