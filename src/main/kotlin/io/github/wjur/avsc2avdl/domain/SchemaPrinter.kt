package io.github.wjur.avsc2avdl.domain


class SchemaPrinter {
    fun writeString(schema: Schema): String {
        return """${schema.writeNamespaceString()}
protocol ${schema.name} {
${schema.writeDocString(0)}${tabs(0)}record ${schema.name} {
${schema.fields.writeFieldsString(1)}
${tabs(0)}}${schema.fields.writeTypesString(0)}
}
"""
    }
}

private fun UnionTypeDef.subRecords(): Sequence<PrintableClass> {
    return this.types.asSequence()
        .flatMap { it.subRecords() }
}

private fun RecordTypeDef.subRecords(): Sequence<PrintableClass> {
    return sequenceOf(PrintableRecord(this)) + this.fields.asSequence()
        .flatMap { it.type.subRecords() }
}

private fun ArrayTypeDef.subRecords(): Sequence<PrintableClass> {
    return itemType.subRecords()
}

private fun MapTypeDef.subRecords(): Sequence<PrintableClass> {
    return valueType.subRecords()
}

private fun TypeDef.subRecords(): Sequence<PrintableClass> {
    return when (this) {
        is ReferenceByNameTypeDef,
        NullTypeDef,
        is IntTypeDef,
        is LongTypeDef,
        is StringTypeDef,
        is BooleanTypeDef -> emptySequence()

        is EnumTypeDef -> sequenceOf(PrintableEnum(this))
        is UnionTypeDef -> this.subRecords()
        is RecordTypeDef -> this.subRecords()
        is MapTypeDef -> this.subRecords()
        is ArrayTypeDef -> this.subRecords()
    }
}

private fun List<Field>.writeTypesString(level: Int): String {
    return this.asSequence()
        .flatMap { it.type.subRecords() }
        .map { it.writeString(level) }
        .joinToString("\n\n")
        .takeIf { it.isNotEmpty() }
        ?.let { "\n\n$it" }
        ?: ""
}

private fun Documentable.writeDocString(level: Int): String {
    return if (this.documentation != null) "${tabs(level)}/** ${this.documentation} */\n" else ""
}

private fun Schema.writeNamespaceString(): String {
    return if (this.namespace != null) """@namespace("${this.namespace}")""" else ""
}

private fun List<Field>.writeFieldsString(level: Int): String {
    return this.joinToString("\n") {
        """${it.writeDocString(level)}${tabs(level)}${it.writeTypeName()}${it.writeUserDataType()} ${it.name}${it.writeDefault()};"""
    }
}

private fun Field.writeDefault(): String {
    return when (default) {
        null -> ""
        is DefaultNull -> " = null"
        is DefaultString -> " = \"${default.value}\""
        is DefaultNumber -> " = ${default.value}"
        is DefaultBoolean -> " = ${default.value}"
        is DefaultEmptyMap -> " = {}"
        is DefaultEmptyArray -> " = []"
    }
}

private fun Field.writeTypeName(): String {
    return writeTypeName(type)
}

private fun Field.writeUserDataType(): String = userDataType?.let { " @userDataType(\"${it.value}\")" } ?: ""
private fun TypeDef.writeJavaClass(): String {
    val annotations = when (this) {
        NullTypeDef,
        is UnionTypeDef,
        is RecordTypeDef,
        is EnumTypeDef,
        is ReferenceByNameTypeDef -> null

        is IntTypeDef -> this.stringableJavaClass?.let { "@java-class(\"$it\")" }
        is LongTypeDef -> this.stringableJavaClass?.let { "@java-class(\"$it\")" }
        is StringTypeDef -> this.stringableJavaClass?.let { "@java-class(\"$it\")" }
        is BooleanTypeDef -> this.stringableJavaClass?.let { "@java-class(\"$it\")" }
        is MapTypeDef ->
            sequenceOf(
                this.stringableKeyJavaClass?.let { "@java-key-class(\"$it\")" },
                this.stringableJavaClass?.let { "@java-class(\"$it\")" }
            ).filterNotNull().joinToString(" ").takeIf { it.isNotEmpty() }

        is ArrayTypeDef -> sequenceOf(
            this.stringableKeyJavaClass?.let { "@java-key-class(\"$it\")" },
            this.stringableJavaClass?.let { "@java-class(\"$it\")" }
        ).filterNotNull().joinToString(" ").takeIf { it.isNotEmpty() }
    }
    return annotations?.let { "$it " } ?: ""
}

private fun writeTypeName(type1: TypeDef): String {
    return type1.writeJavaClass() + when (type1) {
        NullTypeDef -> "null"
        is IntTypeDef -> "int"
        is LongTypeDef -> "long"
        is StringTypeDef -> "string"
        is BooleanTypeDef -> "boolean"
        is UnionTypeDef -> """union { ${type1.types.joinToString(", ") { writeTypeName(it) }} }"""
        is RecordTypeDef -> type1.name
        is ReferenceByNameTypeDef -> type1.name
        is MapTypeDef -> "map<${writeTypeName(type1.valueType)}>"
        is ArrayTypeDef -> "array<${writeTypeName(type1.itemType)}>"
        is EnumTypeDef -> type1.name
    }
}

private fun tabs(level: Int): String = (0..level).joinToString("") { "    " }

private interface PrintableClass {
    fun writeString(level: Int): String
}

data class PrintableRecord(val record: RecordTypeDef) : PrintableClass {
    override fun writeString(level: Int): String {
        return """${record.writeDocString(level)}${tabs(level)}record ${record.name} {
${record.fields.writeFieldsString(level + 1)}
${tabs(level)}}"""
    }
}

data class PrintableEnum(val enum: EnumTypeDef) : PrintableClass {
    override fun writeString(level: Int): String {
        return """${enum.writeDocString(level)}${tabs(level)}enum ${enum.name} {
${enum.symbols.writeSymbolsString(level + 1)}
${tabs(level)}}"""
    }

}

private fun List<String>.writeSymbolsString(level: Int): String {
    return this.joinToString(",\n") { symbolName -> """${tabs(level)}${symbolName}""" }
}
