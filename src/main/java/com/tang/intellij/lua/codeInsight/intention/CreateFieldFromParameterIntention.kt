/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.codeInsight.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.tang.intellij.lua.codeInsight.template.macro.SuggestTypeMacro
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.guessParentClass
import org.jetbrains.annotations.Nls

/**
 *
 * Created by tangzx on 2017/2/11.
 */
class CreateFieldFromParameterIntention : BaseIntentionAction() {
    @Nls
    override fun getFamilyName(): String {
        return "Create field for parameter"
    }

    override fun getText(): String {
        return familyName
    }

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
        val paramDef = getLuaParamDef(editor, psiFile) ?: return false
        var parent: PsiElement? = paramDef.parent ?: return false
        parent = parent!!.parent
        return parent is LuaClassMethodDefStat
    }

    private fun getLuaParamDef(editor: Editor, psiFile: PsiFile): LuaParamDef? {
        val offset = editor.caretModel.offset
        return LuaPsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, LuaParamDef::class.java, false)
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val paramDef = getLuaParamDef(editor, psiFile)
        if (paramDef != null) {
            val methodDef = PsiTreeUtil.getParentOfType(paramDef, LuaClassMethodDefStat::class.java)
            if (methodDef != null) {
                val block = PsiTreeUtil.getChildOfType(methodDef.funcBody, LuaBlock::class.java)!!

                ApplicationManager.getApplication().invokeLater {
                    val paramName = paramDef.text
                    val dialog = CreateFieldFromParameterDialog(project, paramName)
                    if (!dialog.showAndGet()) {
                        return@invokeLater
                    }

                    val fieldName = dialog.fieldName
                    val createDoc = dialog.isCreateDoc
                    if (createDoc) {
                        val context = SearchContext.get(project)
                        val classType = methodDef.guessParentClass(context)
                        if (classType != null) {
                            val def = LuaShortNamesManager.getInstance(project).findClass(context, classType.className)
                            if (def != null) {
                                val tempString = String.format("\n---@field public %s \$type$\$END$", fieldName)
                                val templateManager = TemplateManager.getInstance(project)
                                val template = templateManager.createTemplate("", "", tempString)
                                template.addVariable("type", MacroCallNode(SuggestTypeMacro()), TextExpression("table"), true)
                                template.isToReformat = true

                                val textRange = def.textRange
                                editor.caretModel.moveToOffset(textRange.endOffset)
                                templateManager.startTemplate(editor, template, object : TemplateEditingAdapter() {
                                    override fun templateFinished(template: Template, brokenOff: Boolean) {
                                        insertFieldAssign(project, editor, block, paramName, fieldName)
                                    }
                                })
                                return@invokeLater
                            }
                        }
                    }

                    insertFieldAssign(project, editor, block, paramName, fieldName)
                }
            }
        }
    }

    private fun insertFieldAssign(project: Project, editor: Editor, block: LuaBlock?, paramName: String, fieldName: String) {
        val tempString = String.format("\nself.%s = %s\$END$", fieldName, paramName)
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "", tempString)
        template.isToReformat = true

        editor.caretModel.moveToOffset(block!!.textOffset)
        templateManager.startTemplate(editor, template)
    }
}
