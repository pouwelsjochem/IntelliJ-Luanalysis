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

package com.tang.intellij.lua.comment

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.psi.*

/**
 *
 * Created by TangZX on 2016/11/24.
 */
object LuaCommentUtil {
    fun findOwner(element: LuaDocPsiElement): LuaCommentOwner? {
        val comment = findContainer(element)
        val parent = comment.parent as? LuaCommentOwner

        val closure = when (parent) {
            is LuaLocalDefStat -> {
                if (parent.localDefList.size == 1) {
                    parent.exprList?.expressionList?.firstOrNull() as? LuaClosureExpr
                } else {
                    null
                }
            }
            is LuaAssignStat -> {
                if (parent.varExprList.expressionList.size == 1) {
                    parent.valueExprList?.expressionList?.firstOrNull() as? LuaClosureExpr
                } else {
                    null
                }
            }
            is LuaTableField -> {
                return parent.valueExpr as? LuaClosureExpr
            }
            else -> parent
        }

        if (closure != null && comment.isFunctionImplementation) {
            return closure
        }

        return parent
    }

    fun findContainer(psi: LuaDocPsiElement): LuaComment {
        var element = psi
        while (true) {
            if (element is LuaComment) {
                return element
            }
            element = element.parent as LuaDocPsiElement
        }
    }

    fun findComment(element: LuaCommentOwner): LuaComment? {
        val firstChild = element.firstChild // Left bound comment

        if (firstChild is LuaComment) {
            return firstChild
        }

        val lastChild = element.lastChild // Right bound comment

        if (lastChild is LuaComment) {
            return lastChild
        }

        if (element is LuaClosureExpr) {
            val parent = element.parent

            if (parent is LuaTableField) {
                // If the closure is being assigned to table field, use the field's comment
                return parent.comment
            }

            val grandParent = element.parent.parent
            val assignmentComment = when (grandParent) {
                is LuaLocalDefStat -> if (grandParent.localDefList.size == 1) grandParent.comment else null
                is LuaAssignStat -> if (grandParent.varExprList.expressionList.size == 1) grandParent.comment else null
                else -> null
            }

            if (assignmentComment?.isFunctionImplementation == true) {
                // If the closure is a value of a (single value) assignment statement, use that comment.
                return assignmentComment
            }
        }

        return null
    }

    fun insertTemplate(commentOwner: LuaCommentOwner, editor: Editor, action:(TemplateManager, Template) -> Unit) {
        val comment = commentOwner.comment
        val project = commentOwner.project

        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        if (comment != null)
            template.addTextSegment("\n")

        action(templateManager, template)
        //template.addTextSegment(String.format("---@param %s ", parDef.name))
        //val name = MacroCallNode(SuggestTypeMacro())
        //template.addVariable("type", name, TextExpression("table"), true)
        //template.addEndVariable()

        if (comment != null) {
            editor.caretModel.moveToOffset(comment.textOffset + comment.textLength)
        } else {
            editor.caretModel.moveToOffset(commentOwner.node.startOffset)
            template.addTextSegment("\n")
        }

        templateManager.startTemplate(editor, template)
    }
}
