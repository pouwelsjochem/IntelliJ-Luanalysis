// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.tang.intellij.lua.psi.LuaTypeField;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.StubBasedPsiElement;
import com.tang.intellij.lua.stubs.LuaDocTagFieldStub;
import com.intellij.navigation.ItemPresentation;
import com.tang.intellij.lua.psi.Visibility;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public interface LuaDocTagField extends LuaTypeField, LuaDocPsiElement, PsiNameIdentifierOwner, LuaDocTag, StubBasedPsiElement<LuaDocTagFieldStub> {

  @Nullable
  LuaDocAccessModifier getAccessModifier();

  @Nullable
  LuaDocCommentString getCommentString();

  @NotNull
  List<LuaDocTy> getTyList();

  @Nullable
  LuaDocTypeRef getTypeRef();

  @Nullable
  PsiElement getId();

  @NotNull
  ITy guessParentType(@NotNull SearchContext context);

  @NotNull
  Visibility getVisibility();

  @Nullable
  PsiElement getNameIdentifier();

  @NotNull
  PsiElement setName(@NotNull String newName);

  @Nullable
  String getName();

  @Nullable
  LuaDocTy getIndexType();

  @Nullable
  LuaDocTy getValueType();

  int getTextOffset();

  @NotNull
  ItemPresentation getPresentation();

  boolean isDeprecated();

  boolean isExplicitlyTyped();

  @Nullable
  PsiElement getLbrack();

}
