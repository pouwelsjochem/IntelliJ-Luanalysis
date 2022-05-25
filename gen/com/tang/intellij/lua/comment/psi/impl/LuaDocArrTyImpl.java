// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.tang.intellij.lua.comment.psi.LuaDocTypes.*;
import com.tang.intellij.lua.comment.psi.*;
import com.tang.intellij.lua.psi.Visibility;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public class LuaDocArrTyImpl extends LuaDocTyImpl implements LuaDocArrTy {

  public LuaDocArrTyImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull LuaDocVisitor visitor) {
    visitor.visitArrTy(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaDocVisitor) accept((LuaDocVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public LuaDocTy getTy() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, LuaDocTy.class));
  }

  @Override
  @NotNull
  public ITy getType() {
    return LuaDocPsiImplUtilKt.getType(this);
  }

  @Override
  @NotNull
  public Visibility getVisibility() {
    return LuaDocPsiImplUtilKt.getVisibility(this);
  }

  @Override
  @NotNull
  public ITy guessIndexType(@NotNull SearchContext context) {
    return LuaDocPsiImplUtilKt.guessIndexType(context, this);
  }

  @Override
  @NotNull
  public ITy guessType(@NotNull SearchContext context) {
    return LuaDocPsiImplUtilKt.guessType(context, this);
  }

  @Override
  @NotNull
  public ITy guessParentType(@NotNull SearchContext context) {
    return LuaDocPsiImplUtilKt.guessParentType(context, this);
  }

  @Override
  public boolean isDeprecated() {
    return LuaDocPsiImplUtilKt.isDeprecated(this);
  }

  @Override
  public boolean isExplicitlyTyped() {
    return LuaDocPsiImplUtilKt.isExplicitlyTyped(this);
  }

}
