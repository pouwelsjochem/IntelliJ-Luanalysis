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

package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringFactory
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.lang.LuaFileType
import com.tang.intellij.lua.psi.*
import org.jetbrains.annotations.Nls

/**
 *
 * Created by TangZX on 2017/2/8.
 */
class RemoveUnusedLocal : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        if (session.file.name.matches(LuaFileType.DEFINITION_FILE_REGEX)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : LuaVisitor() {

            override fun visitParamDef(o: LuaParamDef) {
                if (o.textMatches(Constants.WORD_UNDERLINE))
                    return
                val search = ReferencesSearch.search(o, o.useScope)
                var found = false
                for (reference in search) {
                    if (reference.element !is LuaDocPsiElement) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    holder.registerProblem(o,
                            "Unused parameter : '${o.name}'",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            RenameToUnderlineFix())
                }
            }

            override fun visitLocalDefStat(o: LuaLocalDefStat) {
                o.localDefList.forEach { localDef ->
                    if (localDef.name != Constants.WORD_UNDERLINE) {
                        val search = ReferencesSearch.search(localDef, localDef.useScope)
                        if (search.findFirst() == null) {
                            if (o.localDefList.size == 1) {
                                val offset = localDef.node.startOffset - o.node.startOffset
                                val textRange = TextRange(offset, offset + localDef.textLength)
                                holder.registerProblem(o,
                                        "Unused local : '${localDef.text}'",
                                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                        textRange,
                                        RemoveFix("Remove unused local '${localDef.text}'"),
                                        RenameToUnderlineFix())
                            } else {
                                holder.registerProblem(localDef,
                                        "Unused local : '${localDef.text}'",
                                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                        RenameToUnderlineFix())
                            }
                        }
                    }
                }
            }

            override fun visitLocalFuncDefStat(o: LuaLocalFuncDefStat) {
                val name = o.nameIdentifier

                if (name != null) {
                    val search = ReferencesSearch.search(o, o.useScope)
                    if (search.findFirst() == null) {
                        val offset = name.node.startOffset - o.node.startOffset
                        val textRange = TextRange(offset, offset + name.textLength)

                        holder.registerProblem(o,
                                "Unused local function : '${name.text}'",
                                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                textRange,
                                RemoveFix("Remove unused local function : '${name.text}'"))
                    }
                }
            }
        }
    }

    private inner class RenameToUnderlineFix : LocalQuickFix {
        override fun getFamilyName() = "Rename to '${Constants.WORD_UNDERLINE}'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            ApplicationManager.getApplication().invokeLater {
                val factory = RefactoringFactory.getInstance(project)
                val refactoring = factory.createRename(descriptor.psiElement, Constants.WORD_UNDERLINE, false, false)
                refactoring.run()
            }
        }
    }

    private inner class RemoveFix(private val familyName: String) : LocalQuickFix {

        @Nls
        override fun getFamilyName() = familyName

        override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
            val element = problemDescriptor.endElement
            element.delete()
        }
    }
}
