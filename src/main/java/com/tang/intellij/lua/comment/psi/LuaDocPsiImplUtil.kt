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

@file:Suppress("UNUSED_PARAMETER")

package com.tang.intellij.lua.comment.psi

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.reference.LuaClassNameReference
import com.tang.intellij.lua.comment.reference.LuaDocParamNameReference
import com.tang.intellij.lua.comment.reference.LuaDocSeeReference
import com.tang.intellij.lua.lang.type.LuaNumber
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.search.withRecursionGuard
import com.tang.intellij.lua.ty.*
import javax.swing.Icon

/**

 * Created by TangZX on 2016/11/24.
 */
fun getReference(paramNameRef: LuaDocParamNameRef): PsiReference {
    return LuaDocParamNameReference(paramNameRef)
}

fun getReference(docClassNameRef: LuaDocClassNameRef): PsiReference {
    return LuaClassNameReference(docClassNameRef)
}

fun resolveType(nameRef: LuaDocClassNameRef, context: SearchContext): ITy {
    if (nameRef.id.text == Constants.WORD_SELF) {
        val contextClass = LuaPsiTreeUtil.findContextClass(nameRef, context) as? ITyClass
        return if (contextClass != null) TyClass.createSelfType(contextClass) else Ty.UNKNOWN
    }

    return LuaPsiTreeUtil.findGenericDef(nameRef.text, nameRef)?.type ?: Ty.create(nameRef.text)
}

fun getName(identifierOwner: PsiNameIdentifierOwner): String? {
    val id = identifierOwner.nameIdentifier
    return id?.text
}

fun setName(identifierOwner: PsiNameIdentifierOwner, newName: String): PsiElement {
    val oldId = identifierOwner.nameIdentifier
    if (oldId != null) {
        val newId = LuaElementFactory.createDocIdentifier(identifierOwner.project, newName)
        oldId.replace(newId)
        return newId
    }
    return identifierOwner
}

fun getTextOffset(identifierOwner: PsiNameIdentifierOwner): Int {
    val id = identifierOwner.nameIdentifier
    return id?.textOffset ?: identifierOwner.node.startOffset
}

fun getNameIdentifier(tagField: LuaDocTagField): PsiElement? {
    return tagField.id
}

fun getNameIdentifier(tagClass: LuaDocTagClass): PsiElement {
    return tagClass.id
}

fun getIndexType(tagField: LuaDocTagField): LuaDocTy? {
    return if (tagField.lbrack != null) tagField.tyList.firstOrNull() else null
}

fun getValueType(tagField: LuaDocTagField): LuaDocTy? {
    return tagField.tyList.getOrNull(if (tagField.lbrack != null) 1 else 0)
}

fun guessType(tagField: LuaDocTagField, context: SearchContext): ITy {
    val stub = tagField.stub
    if (stub != null)
        return stub.valueTy
    return tagField.valueType?.getType() ?: Ty.UNKNOWN
}

fun guessParentType(tagField: LuaDocTagField, context: SearchContext): ITy {
    val parent = tagField.parent
    val classDef = PsiTreeUtil.findChildOfType(parent, LuaDocTagClass::class.java)
    return classDef?.type ?: Ty.UNKNOWN
}

fun getVisibility(tagField: LuaDocTagField): Visibility {
    val stub = tagField.stub
    if (stub != null)
        return stub.visibility

    val v = tagField.accessModifier?.let { Visibility.get(it.text) }
    return v ?: Visibility.PUBLIC
}

/**
 * 猜测参数的类型
 * @param tagParamDec 参数定义
 * *
 * @return 类型集合
 */
fun getType(tagParamDec: LuaDocTagParam): ITy {
    return tagParamDec.ty?.getType()?.let { ty ->
        val substitutor = SearchContext.withDumb(tagParamDec.project, null) { context ->
            LuaCommentUtil.findContainer(tagParamDec).createSubstitutor(context)
        }

        if (substitutor != null) {
            ty.substitute(substitutor)
        } else ty
    } ?: Ty.UNKNOWN
}

fun getType(vararg: LuaDocTagVararg): ITy {
    return vararg.ty?.getType() ?: Ty.UNKNOWN
}

fun getType(vararg: LuaDocVarargParam): ITy {
    return vararg.ty?.getType() ?: Ty.UNKNOWN
}

fun getType(returnList: LuaDocReturnList): ITy {
    val list = returnList.typeList.tyList.map { it.getType() }
    val variadic = returnList.varreturn != null

    return if (list.size == 1 && !variadic) {
        list.first()
    } else {
        TyMultipleResults(list, variadic)
    }
}

private fun getReturnType(functionReturnType: LuaDocFunctionReturnType): ITy? {
    return SearchContext.withDumb(functionReturnType.project, null) { context ->
        functionReturnType.returnListList.fold(null as ITy?, { returnTy, returnList ->
            TyUnion.union(returnTy, getType(returnList), context)
        })
    }
}

fun getType(tagReturn: LuaDocTagReturn): ITy {
    return tagReturn.functionReturnType?.let { getReturnType(it) } ?: Ty.VOID
}

/**
 * 优化：从stub中取名字
 * @param tagClass LuaDocClassDef
 * *
 * @return string
 */
fun getName(tagClass: LuaDocTagClass): String {
    val stub = tagClass.stub
    if (stub != null)
        return stub.className
    return tagClass.id.text
}

fun getName(tagAlias: LuaDocTagAlias): String {
    val stub = tagAlias.stub
    if (stub != null)
        return stub.name
    return tagAlias.id.text
}

/**
 * for Goto Class
 * @param tagClass class def
 * *
 * @return ItemPresentation
 */
fun getPresentation(tagClass: LuaDocTagClass): ItemPresentation {
    return object : ItemPresentation {
        override fun getPresentableText(): String? {
            return tagClass.name
        }

        override fun getLocationString(): String? {
            return tagClass.containingFile.name
        }

        override fun getIcon(b: Boolean): Icon? {
            return AllIcons.Nodes.Class
        }
    }
}

fun getType(tagClass: LuaDocTagClass): ITyClass {
    val stub = tagClass.stub
    return stub?.classType ?: TyPsiDocClass(tagClass)
}

fun getType(genericDef: LuaDocGenericDef): ITyClass {
    return TyGenericParameter(genericDef)
}

fun isDeprecated(tagClass: LuaDocTagClass): Boolean {
    val stub = tagClass.stub
    return stub?.isDeprecated ?: LuaCommentUtil.findContainer(tagClass).isDeprecated
}

fun isShape(tagClass: LuaDocTagClass): Boolean {
    val stub = tagClass.stub
    return stub?.isShape ?: tagClass.shape != null
}

fun getType(tagType: LuaDocTagType, index: Int): ITy {
    val tyList = tagType.typeList?.tyList

    if (tyList == null) {
        return Ty.UNKNOWN
    }

    return tyList.getOrNull(index)?.getType() ?: if (tagType.variadic != null) {
        tyList.last().getType()
    } else {
        Ty.UNKNOWN
    }
}

fun getType(tagType: LuaDocTagType): ITy {
    val list = tagType.typeList?.tyList?.map { it.getType() }
    return if (list == null) {
        Ty.UNKNOWN
    } else if (list.size == 1 && tagType.variadic == null) {
        list.first()
    } else {
        TyMultipleResults(list, tagType.variadic != null)
    }
}

fun getType(tagNot: LuaDocTagNot, index: Int): ITy {
    val tyList = tagNot.typeList?.tyList

    if (tyList == null) {
        return Ty.VOID
    }

    return tyList.getOrNull(index)?.getType() ?: if (tagNot.variadic != null) {
        tyList.last().getType()
    } else {
        Ty.VOID
    }
}

fun getType(tagNot: LuaDocTagNot): ITy {
    val list = tagNot.typeList?.tyList?.map { it.getType() }
    return if (list != null && list.size > 0) {
        if (list.size > 1 || tagNot.variadic != null) {
            TyMultipleResults(list, tagNot.variadic != null)
        } else {
            list.first()
        }
    } else {
        Ty.VOID
    }
}

@Suppress("UNUSED_PARAMETER")
fun toString(stubElement: StubBasedPsiElement<out StubElement<*>>): String {
    return "[STUB]"// + stubElement.getNode().getElementType().toString();
}

fun getName(tagField: LuaDocTagField): String? {
    val stub = tagField.stub
    if (stub != null)
        return stub.name
    return getName(tagField as PsiNameIdentifierOwner)
}

fun getFieldName(tagField: LuaDocTagField): String? {
    val stub = tagField.stub
    if (stub != null)
        return stub.name
    return tagField.name
}

fun getPresentation(tagField: LuaDocTagField): ItemPresentation {
    return object : ItemPresentation {
        override fun getPresentableText(): String? {
            return tagField.name
        }

        override fun getLocationString(): String? {
            return tagField.containingFile.name
        }

        override fun getIcon(b: Boolean): Icon? {
            return AllIcons.Nodes.Field
        }
    }
}

fun getType(luaDocArrTy: LuaDocArrTy): ITy {
    val baseTy = luaDocArrTy.ty.getType()
    return TyArray(baseTy)
}

fun getType(luaDocGeneralTy: LuaDocGeneralTy): ITy {
    return withRecursionGuard("getType", luaDocGeneralTy) {
        SearchContext.withDumb(luaDocGeneralTy.project, null) {
            resolveType(luaDocGeneralTy.classNameRef, it)
        }
    } ?: TyLazyClass(luaDocGeneralTy.classNameRef.id.text)
}

fun getType(luaDocFunctionTy: LuaDocFunctionTy): ITy {
    return TyDocPsiFunction(luaDocFunctionTy)
}

fun getParams(luaDocFunctionTy: LuaDocFunctionTy): Array<LuaParamInfo>? {
    return luaDocFunctionTy.functionParams?.let  {
        it.functionParamList.map {
            LuaParamInfo(it.id.text, it.ty?.getType() ?: Ty.UNKNOWN)
        }.toTypedArray()
    }
}

fun getVarargParam(luaDocFunctionTy: LuaDocFunctionTy): ITy? {
    return luaDocFunctionTy.functionParams?.varargParam?.type
}

fun getReturnType(luaDocFunctionTy: LuaDocFunctionTy): ITy? {
    return luaDocFunctionTy.functionReturnType?.let { getReturnType(it) }
}

fun getType(luaDocGenericTy: LuaDocGenericTy): ITy {
    val paramTys = luaDocGenericTy.tyList.map { it.getType() }.toTypedArray()
    val baseTy = withRecursionGuard("getType", luaDocGenericTy) {
        SearchContext.withDumb(luaDocGenericTy.project, null) {
            luaDocGenericTy.classNameRef.resolveType(it)
        }
    } ?: TyLazyClass(luaDocGenericTy.classNameRef.id.text)
    return TyGeneric(paramTys, baseTy)
}

fun getType(luaDocParTy: LuaDocParTy): ITy {
    return luaDocParTy.ty.getType()
}

fun getType(booleanLiteral: LuaDocBooleanLiteralTy): ITy {
    return TyPrimitiveLiteral.getTy(TyPrimitiveKind.Boolean, booleanLiteral.value.text)
}

fun getType(numberLiteral: LuaDocNumberLiteralTy): ITy {
    val n = LuaNumber.getValue(numberLiteral.number.text)
    val valueString = if (numberLiteral.negative != null) "-${n}" else n.toString()
    return if (n != null) {
        TyPrimitiveLiteral.getTy(TyPrimitiveKind.Number, valueString)
    } else Ty.UNKNOWN
}

fun getType(stringLiteral: LuaDocStringLiteralTy): ITy {
    return TyPrimitiveLiteral.getTy(TyPrimitiveKind.String, LuaString.getContent(stringLiteral.value.text).value)
}

fun getType(snippet: LuaDocSnippetTy): ITy {
    return TySnippet(snippet.content.text)
}

fun getType(unionTy: LuaDocUnionTy): ITy {
    return unionTy.tyList.fold<LuaDocTy, ITy?>(null, { ty, docTy ->
        SearchContext.withDumb(unionTy.project, null) { context ->
            TyUnion.union(ty, docTy.getType(), context)
        }
    }) ?: Ty.UNKNOWN
}

fun getReference(see: LuaDocTagSee): PsiReference? {
    if (see.id == null) return null
    return LuaDocSeeReference(see)
}

fun getType(tbl: LuaDocTableTy): ITy {
    return TyDocTable(tbl.tableDef)
}

fun guessParentType(f: LuaDocTableField, context: SearchContext): ITy {
    val p = f.parent as LuaDocTableDef
    return TyDocTable(p)
}

fun getVisibility(f: LuaDocTableField): Visibility {
    return Visibility.PUBLIC
}

fun getNameIdentifier(f: LuaDocTableField): PsiElement? {
    return f.id
}

fun getName(f:LuaDocTableField): String? {
    val stub = f.stub
    if (stub != null)
        return stub.name
    return getName(f as PsiNameIdentifierOwner)
}

fun getIndexType(f: LuaDocTableField): LuaDocTy? {
    return if (f.lbrack != null) f.tyList.firstOrNull() else null
}

fun getValueType(f: LuaDocTableField): LuaDocTy? {
    return f.tyList.getOrNull(if (f.lbrack != null) 1 else 0)
}

fun guessType(f:LuaDocTableField, context: SearchContext): ITy {
    val stub = f.stub
    if (stub != null)
        return stub.valueTy ?: Ty.UNKNOWN
    return f.valueType?.getType() ?: Ty.UNKNOWN
}

fun getNameIdentifier(g: LuaDocGenericDef): PsiElement? {
    return g.id
}

fun isDeprecated(member: LuaClassMember): Boolean {
    return false
}

fun getNameIdentifier(g: LuaDocTagAlias): PsiElement {
    return g.id
}

fun getType(alias: LuaDocTagAlias): ITy {
    val stub = alias.stub
    return stub?.type ?: TyAlias(alias.name, alias.genericDefList.map { TyGenericParameter(it) }.toTypedArray(), alias.ty?.getType() ?: Ty.UNKNOWN)
}
