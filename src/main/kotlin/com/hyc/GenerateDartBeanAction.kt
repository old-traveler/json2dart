package com.hyc

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.hyc.ui.JsonInputDialog
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import java.lang.StringBuilder
import java.util.regex.Pattern

@Suppress("UNCHECKED_CAST")
class GenerateDartBeanAction : AnAction() {

    private val dartClassList = mutableListOf<DartClass>()
    override fun actionPerformed(event: AnActionEvent) {
        dartClassList.clear()
        val jsonString: String
        try {
            val project = event.getData(PlatformDataKeys.PROJECT) ?: return
            val inputDialog = JsonInputDialog("", project)
            inputDialog.show()
            val className = inputDialog.getClassName()
            val inputString = inputDialog.inputString.takeIf { it.isNotEmpty() } ?: return

            jsonString = inputString
            val linkedTreeMap = Gson().fromJson<LinkedTreeMap<String, Any?>>(jsonString, LinkedTreeMap::class.java)
            generateDartBean(className, linkedTreeMap)
            val targetList = dartClassList.reversed()
            val sb = StringBuilder()
            sb.append("import 'package:flutter_beike_parse_helper_package/annotation/json_parse_annotation.dart';\n" +
                    "import 'package:flutter_beike_parse_helper_package/helper/parse_helper.dart';\n")

            val file = event.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
            sb.appendln("part '${file.name.replaceFirst(".dart", "")}.parse.dart';")
            targetList.forEach {
                sb.appendln()
                sb.appendln("@JsonClass()")
                sb.appendln("class ${it.className} {")
                it.list.forEach { field ->
                    sb.appendln("  @JsonField([\"${field.parseKey}\"])")
                    sb.append("  ").append(field.className).append(" ").append(field.fieldName).appendln(";")
                }
                sb.appendln()
                sb.appendln("  ${it.className}();")
                sb.appendln()
                sb.appendln("  factory ${it.className}.fromJson(Map<String, dynamic> json) {")
                sb.appendln("    return _\$${it.className}FromJson(json);")
                sb.appendln("  }")

                sb.appendln()

                sb.appendln("  Map<String, dynamic> toJson() {")
                sb.appendln("    return _\$${it.className}ToJson(this);")
                sb.appendln("  }")




                sb.appendln("}")
            }
            insertCode(event, sb.toString())

        } catch (e: Throwable) {
            throw e
        }

    }

    private fun insertCode(e: AnActionEvent, code: String) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.project
        val document = editor.document

        val offset = 0
        val offsetEnd = code.length + offset
        //Making the replacement
        WriteCommandAction.runWriteCommandAction(project
        ) {
            document.insertString(offset, code)
            val file = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document)
            editor.selectionModel.selectLineAtCaret()

            val processor = ReformatCodeProcessor(project, file, TextRange(offset, offsetEnd), false)
            processor.runWithoutProgress()
        }

    }


    private fun generateDartBean(className: String, linkedTreeMap: LinkedTreeMap<String, Any?>) {
        val fieldList = mutableListOf<FieldClass>()
        val dartClass = DartClass(className, fieldList)
        linkedTreeMap.entries.forEach {
            when {
                it.value == null -> fieldList.add(FieldClass("dynamic", lineToHump(it.key), it.key))
                it.value is List<*> -> {
                    val sb = StringBuilder()
                    var value = it.value
                    while (value is List<*>) {
                        sb.insert(sb.lastIndexOf("<") + 1, "List<>")
                        value = value.firstOrNull()
                    }
                    if (value != null && getBaseType(value) != null) {
                        sb.insert(sb.lastIndexOf("<") + 1, getBaseType(value))
                    } else if (value is LinkedTreeMap<*, *>) {
                        val name = className + classLineToHump(it.key)
                        sb.insert(sb.lastIndexOf("<") + 1, name)
                        generateDartBean(name, value as LinkedTreeMap<String, Any?>)
                    } else {
                        sb.insert(sb.lastIndexOf("<") + 1, "dynamic")
                    }
                    fieldList.add(FieldClass(sb.toString(), lineToHump(it.key), it.key))
                }
                getBaseType(it.value!!) != null -> fieldList.add(FieldClass(getBaseType(it.value!!)!!, lineToHump(it.key), it.key))
                it.value is LinkedTreeMap<*, *> -> {
                    val name = className + classLineToHump(it.key)
                    fieldList.add(FieldClass(name, lineToHump(it.key), it.key))
                    generateDartBean(name, it.value as LinkedTreeMap<String, Any?>)
                }
                else -> fieldList.add(FieldClass("dynamic", lineToHump(it.key), it.key))
            }
        }
        dartClassList.add(dartClass)

    }

    private fun getBaseType(any: Any): String? {
        return when (any) {
            is Int -> "int"
            is Long -> "int"
            is Float -> if (any - any.toInt() != 0.0f) "double" else "int"
            is Double ->
                if (any - any.toInt() != 0.0)
                    "double"
                else
                    "int"
            is String -> "String"
            is Boolean -> "bool"
            else -> null
        }
    }
}

fun classLineToHump(str: String): String {
    val name = lineToHump(str)
    return name.replaceFirst(name.first(), name.first().toUpperCase())
}

fun lineToHump(source: String): String {
    var str = source
    if (!str.contains("_")) {
        return str
    }
    str = str.toLowerCase()
    val matcher = Pattern.compile("_(\\w)").matcher(str)
    val sb = StringBuffer()
    while (matcher.find()) {
        matcher.appendReplacement(sb, matcher.group(1).toUpperCase())
    }
    matcher.appendTail(sb)
    return sb.toString()
}


data class DartClass(val className: String, val list: List<FieldClass>)

data class FieldClass(val className: String, val fieldName: String, val parseKey: String)