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

package com.tang.intellij.lua.comment.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocTypeRef
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.psi.LuaScopedTypeTree
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext

/**

 * Created by TangZX on 2016/11/29.
 */
class LuaTypeReference(element: LuaDocTypeRef) : PsiReferenceBase<LuaDocTypeRef>(element) {

    override fun getRangeInElement() = TextRange(0, myElement.textLength)

    override fun isReferenceTo(element: PsiElement): Boolean {
        return myElement.manager.areElementsEquivalent(element, resolve())
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = LuaElementFactory.createWith(myElement.project, "---@type $newElementName")
        val typeRef = PsiTreeUtil.findChildOfType(element, LuaDocTypeRef::class.java)
        return myElement.replace(typeRef!!)
    }

    override fun resolve(): PsiElement? {
        val name = myElement.text
        val project = myElement.project

        val context = SearchContext.get(project)
        val scopedType = LuaScopedTypeTree.get(myElement.containingFile)?.findName(context, myElement, name)

        if (scopedType != null) {
            return scopedType
        }

        return LuaShortNamesManager.getInstance(project).findType(context, name)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
