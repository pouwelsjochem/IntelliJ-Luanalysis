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

package com.tang.intellij.lua.psi;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.tang.intellij.lua.comment.psi.LuaDocGenericDef;
import com.tang.intellij.lua.comment.psi.LuaDocTagClass;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.ITyClass;
import com.tang.intellij.lua.ty.Primitives;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * Created by tangzx on 2016/12/3.
 */
public class LuaPsiTreeUtil {

    public static void walkUpLabel(PsiElement current, Processor<LuaLabelStat> processor) {
        PsiElement prev = current.getPrevSibling();
        while (true) {
            if (prev == null)
                prev = current.getParent();
            if (prev == null || prev instanceof PsiFile)
                break;
            if (prev instanceof LuaLabelStat && !processor.process((LuaLabelStat) prev))
                break;
            current = prev;
            prev = prev.getPrevSibling();
        }
    }

    public static <T extends PsiElement> void walkTopLevelInFile(PsiElement element, Class<T> cls, Processor<T> processor) {
        if (element == null || processor == null)
            return;
        PsiElement parent = element;
        while (!(parent.getParent() instanceof PsiFile))
            parent = parent.getParent();

        for(PsiElement child = parent; child != null; child = child.getPrevSibling()) {
            if (cls.isInstance(child)) {
                if (!processor.process(cls.cast(child))) {
                    break;
                }
            }
        }
    }

    @Nullable
    public static <T extends PsiElement> T findElementOfClassAtOffset(@NotNull PsiFile file, int offset, @NotNull Class<T> clazz, boolean strictStart) {
        T t = PsiTreeUtil.findElementOfClassAtOffset(file, offset, clazz, strictStart);
        if (t == null)
            t = PsiTreeUtil.findElementOfClassAtOffset(file, offset - 1, clazz, strictStart);
        return t;
    }

    public static <T extends PsiElement> T findAncestorOfType(@Nullable PsiElement element, @NotNull Class<T> aClass, @NotNull Class... skips) {
        if (element == null) {
            return null;
        } else {
            element = element.getParent();

            while (element != null && (!aClass.isInstance(element) || PsiTreeUtil.instanceOf(element, skips))) {
                if (element instanceof PsiFile) {
                    return null;
                }

                element = element.getParent();
            }

            return aClass.cast(element);
        }
    }

    @Nullable
    private static LuaDocGenericDef findOwnerClassGenericDef(LuaFuncBodyOwner<?> funcBodyOwner, String name) {
        SearchContext context = SearchContext.Companion.get(funcBodyOwner.getProject());
        ITy parentType = funcBodyOwner.guessParentType(context);

        if (parentType instanceof ITyClass) {
            ITyClass cls = (ITyClass) parentType;
            LuaClass luaClass = LuaShortNamesManager.Companion.getInstance(context.getProject()).findClass(context, cls.getClassName());

            if (luaClass instanceof LuaDocTagClass) {
                LuaDocTagClass docTagClass = (LuaDocTagClass) luaClass;

                for (LuaDocGenericDef genericDef : docTagClass.getGenericDefList()) {
                    if (name.equals(genericDef.getId().getText()))
                    {
                        return genericDef;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static LuaDocGenericDef findOwnerClassGenericDef(@Nullable LuaCommentOwner commentOwner, String name) {
        if (commentOwner instanceof LuaAssignStat) {
            LuaAssignStat assignStat = (LuaAssignStat) commentOwner;
            LuaExprList exprList = assignStat.getValueExprList();

            if (exprList == null || exprList.getExpressionList().size() != 1) {
                return null;
            }

            commentOwner = exprList.getExpressionList().get(0);
        }

        if (commentOwner instanceof LuaFuncBodyOwner<?>) {
            LuaFuncBodyOwner<?> funcBodyOwner = (LuaFuncBodyOwner<?>) commentOwner;
            return findOwnerClassGenericDef(funcBodyOwner, name);
        }

        return null;
    }

    private static final Class[] WS = {PsiWhiteSpace.class};
    private static final Class[] WS_COMMENTS = {PsiWhiteSpace.class, PsiComment.class};

    public static PsiElement skipWhitespacesBackward(@Nullable PsiElement element) {
        return PsiTreeUtil.skipSiblingsBackward(element, WS);
    }

    public static PsiElement skipWhitespacesAndCommentsBackward(@Nullable PsiElement element) {
        return PsiTreeUtil.skipSiblingsBackward(element, WS_COMMENTS);
    }

    public static PsiElement skipWhitespacesForward(@Nullable PsiElement element) {
        return PsiTreeUtil.skipSiblingsForward(element, WS);
    }

    public static PsiElement skipWhitespacesAndCommentsForward(@Nullable PsiElement element) {
        return PsiTreeUtil.skipSiblingsForward(element, WS_COMMENTS);
    }

    @NotNull
    public static ITy findContextClass(SearchContext context, PsiElement current) {
        ITy ownerTy = LuaScopedTypeTree.Companion.get(current.getContainingFile()).findOwner(context, current);
        return ownerTy != null ? ownerTy : Primitives.Companion.getUNKNOWN();
    }

    @Nullable
    public static LuaClass findClass(SearchContext context, String name) {
        PsiElement contextElement = context.getElement();

        if (contextElement != null) {
            LuaScopedType scopedType = LuaScopedTypeTree.Companion.get(contextElement.getContainingFile()).findName(context, contextElement, name);

            if (scopedType instanceof LuaClass) {
                return (LuaClass) scopedType;
            }
        }

        return LuaShortNamesManager.Companion.getInstance(context.getProject()).findClass(context, name);
    }

    @Nullable
    public static LuaTypeDef findType(SearchContext context, String name) {
        PsiElement contextElement = context.getElement();

        if (contextElement != null) {
            LuaScopedType scopedType = LuaScopedTypeTree.Companion.get(contextElement.getContainingFile()).findName(context, contextElement, name);

            if (scopedType != null) {
                return scopedType;
            }
        }

        return LuaShortNamesManager.Companion.getInstance(context.getProject()).findType(context, name);
    }

    public static void processChildren(PsiElement parent, PsiElementProcessor<PsiElement> processor) {
        PsiElement child = parent.getFirstChild();
        while (child != null) {
            if (processor.execute(child)) {
                child = child.getNextSibling();
            } else break;
        }
    }
}
