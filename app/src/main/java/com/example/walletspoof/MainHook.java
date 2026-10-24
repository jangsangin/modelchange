package com.example.walletspoof;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * com.finshell.wallet 프로세스에서만 모델명을 위조한다.
 * 다른 앱(토스뱅크 등)은 건드리지 않는다.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.finshell.wallet";
    private static final String FAKE_MODEL = "PLK110";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        // 1) android.os.Build.MODEL
        try {
            XposedHelpers.setStaticObjectField(Build.class, "MODEL", FAKE_MODEL);
        } catch (Throwable t) {
            XposedBridge.log("WalletModelSpoof: Build.MODEL failed: " + t);
        }

        // 2) SystemProperties.get(...) 로 읽는 모델 관련 속성
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] instanceof String) {
                        String key = (String) param.args[0];
                        if (isModelKey(key)) {
                            param.setResult(FAKE_MODEL);
                        }
                    }
                }
            };
            XposedBridge.hookAllMethods(sp, "get", hook);
        } catch (Throwable t) {
            XposedBridge.log("WalletModelSpoof: SystemProperties hook failed: " + t);
        }
    }

    private static boolean isModelKey(String key) {
        return "ro.product.model".equals(key)
                || (key.startsWith("ro.product.") && key.endsWith(".model"));
    }
}
