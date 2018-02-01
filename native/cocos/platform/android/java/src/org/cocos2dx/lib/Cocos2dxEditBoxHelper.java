/****************************************************************************
Copyright (c) 2010-2012 cocos2d-x.org
Copyright (c) 2013-2016 Chukong Technologies Inc.
Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Cocos2dxEditBoxHelper {
    private static final String TAG = Cocos2dxEditBoxHelper.class.getSimpleName();
    private static Cocos2dxActivity mCocos2dxActivity;
    private static ResizeLayout mFrameLayout;

    private static SparseArray<Cocos2dxEditBox> mEditBoxArray;
    private static int mViewTag = 0;
    private static float mPadding = 5.0f;

    //Call native methods
    private static native void editBoxEditingDidBegin(int index);
    public static void __editBoxEditingDidBegin(int index){
        editBoxEditingDidBegin(index);
    }

    private static native void editBoxEditingChanged(int index, String text);
    public static void __editBoxEditingChanged(int index, String text){
        editBoxEditingChanged(index, text);
    }

    private static native void editBoxEditingDidEnd(int index, String text);
    public static void __editBoxEditingDidEnd(int index, String text){
        editBoxEditingDidEnd(index, text);
    }

    private static native void editBoxEditingReturn(int index);
    public static void __editBoxEditingReturn(int index) {
        editBoxEditingReturn(index);
    }

    public Cocos2dxEditBoxHelper(ResizeLayout layout) {
        Cocos2dxEditBoxHelper.mFrameLayout = layout;

        Cocos2dxEditBoxHelper.mCocos2dxActivity = (Cocos2dxActivity) Cocos2dxActivity.getContext();
        Cocos2dxEditBoxHelper.mEditBoxArray = new SparseArray<Cocos2dxEditBox>();
    }

    public static int getPadding(float scaleX) {
        Resources r = mCocos2dxActivity.getResources();
        return (int) (mPadding * scaleX);
    }

    public static int createEditBox(final int left, final int top, final int width, final int height, final float scaleX) {
        final int index = mViewTag;
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Cocos2dxEditBox editBox = new Cocos2dxEditBox(mCocos2dxActivity);
                editBox.setFocusable(true);
                editBox.setFocusableInTouchMode(true);
                editBox.setInputFlag(5); //kEditBoxInputFlagLowercaseAllCharacters
                editBox.setInputMode(6); //kEditBoxInputModeSingleLine
                editBox.setReturnType(0);  //kKeyboardReturnTypeDefault
                editBox.setHintTextColor(Color.GRAY);
                editBox.setVisibility(View.GONE);
                editBox.setBackgroundColor(Color.TRANSPARENT);
                editBox.setTextColor(Color.WHITE);
                editBox.setSingleLine();
                editBox.setOpenGLViewScaleX(scaleX);

                editBox.setPadding(getPadding(scaleX),0, 0, 0);

                FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);

                lParams.leftMargin = left;
                lParams.topMargin = top;
                lParams.width = width;
                lParams.height = height;
                lParams.gravity = Gravity.TOP | Gravity.LEFT;

                mFrameLayout.addView(editBox, lParams);
                editBox.setTag(false);
                editBox.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    //http://stackoverflow.com/questions/21713246/addtextchangedlistener-and-ontextchanged-are-always-called-when-android-fragment
                    @Override
                    public void afterTextChanged(final Editable s) {
                        if (!editBox.getChangedTextProgrammatically()) {
                            if((Boolean)editBox.getTag()) {
                                mCocos2dxActivity.runOnGLThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Cocos2dxEditBoxHelper.__editBoxEditingChanged(index, s.toString());
                                    }
                                });
                            }
                        }
                        editBox.setChangedTextProgrammatically(false);

                    }
                });


                editBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {

                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        editBox.setTag(true);
                        editBox.setChangedTextProgrammatically(false);
                        if (hasFocus) {
                            mCocos2dxActivity.runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    Cocos2dxEditBoxHelper.__editBoxEditingDidBegin(index);
                                }
                            });
                            editBox.setSelection(editBox.getText().length());
                            mFrameLayout.setEnableForceDoLayout(true);
                            mCocos2dxActivity.getGLSurfaceView().setSoftKeyboardShown(true);
                            Log.d(TAG, "edit box get focus");
                        } else {
                            editBox.setVisibility(View.GONE);
                            // Note that we must to copy a string to prevent string content is modified
                            // on UI thread while 's.toString' is invoked at the same time.
                            final String text = new String(editBox.getText().toString());
                            mCocos2dxActivity.runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    Cocos2dxEditBoxHelper.__editBoxEditingDidEnd(index, text);
                                }
                            });
                            mCocos2dxActivity.hideVirtualButton();
                            mFrameLayout.setEnableForceDoLayout(false);
                            Log.d(TAG, "edit box lose focus");
                        }
                    }
                });

                editBox.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                                (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            //if editbox doesn't support multiline, just hide the keyboard
                            if ((editBox.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != InputType.TYPE_TEXT_FLAG_MULTI_LINE) {
                                Cocos2dxEditBoxHelper.runEditBoxEditingReturnInGLThread(index);
                                Cocos2dxEditBoxHelper.closeKeyboardOnUiThread(index);
                                return true;
                            } else {
                                Cocos2dxEditBoxHelper.runEditBoxEditingReturnInGLThread(index);
                            }
                        }
                        return false;
                    }
                });


                editBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            Cocos2dxEditBoxHelper.closeKeyboardOnUiThread(index);
                        }
                        return false;
                    }
                });

                mEditBoxArray.put(index, editBox);
            }
        });
        return mViewTag++;
    }

    private static  void runEditBoxEditingReturnInGLThread(final int index) {
        mCocos2dxActivity.runOnGLThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBoxHelper.__editBoxEditingReturn(index);
            }
        });
    }

    public static void removeEditBox(final int index) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    mEditBoxArray.remove(index);
                    mFrameLayout.removeView(editBox);
                }
            }
        });
    }

    public static void setFont(final int index, final String fontName, final float fontSize){
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    Typeface tf;
                    if (!fontName.isEmpty()) {
                        if (fontName.endsWith(".ttf")) {
                            try {
                                tf = Cocos2dxTypefaces.get(mCocos2dxActivity.getContext(), fontName);
                            } catch (final Exception e) {
                                Log.e("Cocos2dxEditBoxHelper", "error to create ttf type face: "
                                        + fontName);
                                // The file may not find, use system font.
                                tf  =  Typeface.create(fontName, Typeface.NORMAL);
                            }
                        } else {
                            tf  =  Typeface.create(fontName, Typeface.NORMAL);
                        }

                    }else{
                        tf = Typeface.DEFAULT;
                    }
                    if (fontSize >= 0){
                        editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
                    }
                    editBox.setTypeface(tf);
                }
            }
        });
    }

    public static void setFontColor(final int index, final int red, final int green, final int blue, final int alpha){
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setTextColor(Color.argb(alpha, red, green, blue));
                }
            }
        });
    }

    public static void setPlaceHolderText(final int index, final String text){
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setHint(text);
                }
            }
        });
    }

    public static void setPlaceHolderTextColor(final int index, final int red, final int green, final int blue, final int alpha){
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setHintTextColor(Color.argb(alpha, red, green, blue));
                }
            }
        });
    }

    public static void setMaxLength(final int index, final int maxLength) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setMaxLength(maxLength);
                }
            }
        });
    }

    public static void setVisible(final int index, final boolean visible) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            }
        });
    }


    public static void setText(final int index, final String text){
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setChangedTextProgrammatically(true);
                    editBox.setText(text);
                    int position = editBox.getText().length();
                    editBox.setSelection(position);
                }
            }
        });
    }

    public static void setReturnType(final int index, final int returnType) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setReturnType(returnType);
                }
            }
        });
    }

    public static void setInputMode(final int index, final int inputMode) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setInputMode(inputMode);
                }
            }
        });
    }

    public static void setInputFlag(final int index, final int inputFlag) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setInputFlag(inputFlag);
                }
            }
        });
    }


    public static void setEditBoxViewRect(final int index, final int left, final int top, final int maxWidth, final int maxHeight) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cocos2dxEditBox editBox = mEditBoxArray.get(index);
                if (editBox != null) {
                    editBox.setEditBoxViewRect(left, top, maxWidth, maxHeight);
                }
            }
        });
    }



    public static void openKeyboard(final int index) {

        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               openKeyboardOnUiThread(index);
            }
        });
    }

    private static void openKeyboardOnUiThread(int index) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "openKeyboardOnUiThread doesn't run on UI thread!");
            return;
        }

        final InputMethodManager imm = (InputMethodManager) mCocos2dxActivity.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Cocos2dxEditBox editBox = mEditBoxArray.get(index);
        if (null != editBox) {
            editBox.requestFocus();
            mCocos2dxActivity.getGLSurfaceView().requestLayout();
            imm.showSoftInput(editBox, 0);
            mCocos2dxActivity.getGLSurfaceView().setSoftKeyboardShown(true);
        }
    }

    private static void closeKeyboardOnUiThread(int index) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "closeKeyboardOnUiThread doesn't run on UI thread!");
            return;
        }
        
        final InputMethodManager imm = (InputMethodManager) mCocos2dxActivity.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Cocos2dxEditBox editBox = mEditBoxArray.get(index);
        if (null != editBox) {
            imm.hideSoftInputFromWindow(editBox.getWindowToken(), 0);
            mCocos2dxActivity.getGLSurfaceView().setSoftKeyboardShown(false);
            mCocos2dxActivity.getGLSurfaceView().requestFocus();
            // can take effect after GLSurfaceView has focus
            mCocos2dxActivity.hideVirtualButton();
        }
    }

    // Note that closeKeyboard will be invoked on GL thread
    public static void closeKeyboard(final int index) {
        mCocos2dxActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeKeyboardOnUiThread(index);
            }
        });
    }
}
