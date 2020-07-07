package com.hyc;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class GenerateDartAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new GenerateDartBeanAction().actionPerformed(e);
    }
}
