/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.vcs.log.ui.filter;

import com.intellij.vcs.log.VcsCommitMetadata;
import com.intellij.vcs.log.VcsLogDetailsFilter;
import com.intellij.vcs.log.VcsLogTextFilter;
import com.intellij.vcs.log.impl.VcsLogUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class VcsLogTextFilterImpl implements VcsLogDetailsFilter, VcsLogTextFilter {

  @NotNull private final String myText;
  private final boolean myIsRegexAllowed;
  private final boolean myMatchCase;

  public VcsLogTextFilterImpl(@NotNull String text, boolean isRegexAllowed, boolean matchCase) {
    myText = text;
    myIsRegexAllowed = isRegexAllowed;
    myMatchCase = matchCase;
  }

  // used in upsource
  @SuppressWarnings("unused")
  public VcsLogTextFilterImpl(@NotNull String text) {
    this(text, false, false);
  }

  @Override
  public boolean matches(@NotNull VcsCommitMetadata details) {
    if (isRegex()) {
      Pattern pattern;
      if (myMatchCase) {
        pattern = Pattern.compile(myText);
      }
      else {
        pattern = Pattern.compile(myText, Pattern.CASE_INSENSITIVE);
      }
      return pattern.matcher(details.getFullMessage()).find();
    }

    if (myMatchCase) return details.getFullMessage().contains(myText);
    return details.getFullMessage().toLowerCase().contains(myText.toLowerCase());
  }

  @Override
  @NotNull
  public String getText() {
    return myText;
  }

  @Override
  public boolean isRegex() {
    return myIsRegexAllowed && VcsLogUtil.isRegexp(myText);
  }

  @Override
  public boolean matchesCase() {
    return myMatchCase;
  }

  @Override
  public String toString() {
    return (isRegex() ? "matching " : "containing ") + myText + " (case " + (myMatchCase ? "sensitive" : "insensitive") + ")";
  }
}
