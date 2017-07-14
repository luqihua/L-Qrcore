# Android中使用的二维码组件  
> zxingcore是在[zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded)这个库的基础上进行二次开发


> zxingcore的使用方法，

```
 /**
     * 在onCreate中调用
     */
        mScannerHelper = new QRScannerHelper(this);
        mScannerHelper.setCallBack(new QRScannerHelper.OnScannerCallBack() {
            @Override
            public void onScannerBack(IntentResult result) {
                Toast.makeText(MainActivity.this, result.getContents(), Toast.LENGTH_SHORT).show();
            }
        });
    
    //在onActivityResult中添加代码
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mScannerHelper != null) {
            mScannerHelper.onActivityResult(requestCode, resultCode, data);
        }
    }
    
        /**
     * 开启扫描界面
     *
     * @param view
     */
    public void start(View view) {
        mScannerHelper.startScanner();
    }
    
    
```
