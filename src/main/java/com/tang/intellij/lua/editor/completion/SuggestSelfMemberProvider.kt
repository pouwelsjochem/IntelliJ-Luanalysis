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

package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.LuaPsiTreeUtil
import com.tang.intellij.lua.psi.LuaClassMethodDefStat
import com.tang.intellij.lua.ty.TypeMember
import com.tang.intellij.lua.ty.guessParentClass
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

/**
 * suggest self.xxx
 * Created by TangZX on 2017/4/11.
 */
class SuggestSelfMemberProvider : ClassMemberCompletionProvider() {
    override fun addCompletions(session: CompletionSession) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet
        val position = completionParameters.position
        val methodDef = PsiTreeUtil.getParentOfType(position, LuaClassMethodDefStat::class.java)
        if (methodDef != null && !methodDef.isStatic) {
            val context = SearchContext.get(position.project)
            methodDef.guessParentClass(context)?.let { type ->
                val contextTy = LuaPsiTreeUtil.findContextClass(context, position)

                type.processMembers(context) { curType, member ->
                    val curClass = (if (curType is ITyGeneric) curType.base else type) as? ITyClass

                    if (curClass != null && member.name != null && curClass.isVisibleInScope(context.project, contextTy, member.visibility)) {
                        addMember(context,
                            completionResultSet,
                            member,
                            curClass.getMemberSubstitutor(context),
                            curClass,
                            member.guessType(context) ?: Primitives.UNKNOWN,
                            MemberCompletionMode.Colon,
                            object : HandlerProcessor() {
                                override fun process(element: LuaLookupElement, member: TypeMember, memberTy: ITy?): LookupElement { return element }

                                override fun processLookupString(lookupString: String, member: TypeMember, memberTy: ITy?): String {
                                    return if (memberTy is ITyFunction) "self:${member.name}" else "self.${member.name}"
                                }
                            })
                    }

                    true
                }
            }
        }
    }
}
