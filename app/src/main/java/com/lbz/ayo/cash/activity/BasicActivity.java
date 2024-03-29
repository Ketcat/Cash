package com.lbz.ayo.cash.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.lbz.ayo.cash.MainActivity;
import com.lbz.ayo.cash.R;
import com.lbz.ayo.cash.bean.LoginBean;
import com.lbz.ayo.cash.utils.Contants;
import com.lbz.ayo.cash.utils.SharedPreUtil;
import com.lbz.ayo.cash.view.LoadingCustom;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by heise on 2018/5/11.
 */

public abstract class BasicActivity extends AppCompatActivity {

    protected Context mContext;
    String[] smsList = {"ID", "CN"};
    boolean isLogin = false;
    protected static final int APP_REQUEST_CODE = 99;
    @SuppressLint("HandlerLeak")
    private Handler handlerBasic = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LoadingCustom.dismissprogress();
            String result = msg.getData().getString("result");
            Log.e("BasicActivity", result);
            if (!TextUtils.isEmpty(result)) {
                Gson gson = new Gson();
                LoginBean loginBean = gson.fromJson(result, LoginBean.class);
                if (TextUtils.isEmpty(loginBean.getError())) {
                    SharedPreUtil.saveSring(mContext, SharedPreUtil.TOKEN, loginBean.getToken());
                    SharedPreUtil.saveBoolean(mContext, SharedPreUtil.ISLOGIN, true);
                    SharedPreUtil.saveLong(mContext,SharedPreUtil.LASTLOGINTIME,System.currentTimeMillis());
                    isLogin = true;
                    startNewActivity();
                    Intent intent = new Intent(Contants.action);
                    intent.putExtra("isShow", false);
                    mContext.sendBroadcast(intent);
                    Toast.makeText(mContext, R.string.login_yes, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, loginBean.getMessage(), Toast.LENGTH_SHORT).show();
                    phoneLogin();
                }
            } else {
                Toast.makeText(mContext, R.string.login_no, Toast.LENGTH_SHORT).show();
                phoneLogin();
            }
        }
    };

    /**
     * 获取布局文件资源ID
     *
     * @return ID
     */
    protected abstract int getResourceId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResourceId());
        mContext = this;
        isLogin = SharedPreUtil.readBoolean(mContext, SharedPreUtil.ISLOGIN);
        init();
        initView();
        initData();
        initListener();
    }

    /**
     * 初始化
     */
    protected abstract void init();

    /**
     * 初始化视图
     */
    protected abstract void initView();

    /**
     * 初始化数据
     */
    protected abstract void initData();

    /**
     * 初始化事件监听
     */
    protected abstract void initListener();

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void phoneLogin() {
        final Intent intent = new Intent(mContext, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder = new
                AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                AccountKitActivity.ResponseType.TOKEN); // or
        // .ResponseType.TOKEN
        //设置facebook登陆页支持哪几个国家
        configurationBuilder.setSMSWhitelist(smsList);
        //设置facebook登陆页面的默认国家
        configurationBuilder.setDefaultCountryCode("ID");
        //设置facebook登陆页面的语言
        Resources resources = mContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(new Locale("ID"));
        } else {
            configuration.locale = new Locale("ID");
        }
        resources.updateConfiguration(configuration, metrics);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }

    protected void GoToMainActivity() {
        // TODO Auto-generated method stub
        phoneLogin();
//        login("8615225578747", "000000");
    }

    protected void startNewActivity() {
        Intent mIntent = new Intent();
        mIntent.setClass(mContext, MainActivity.class);
        startActivity(mIntent);
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent
            data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == APP_REQUEST_CODE) { // confirm that this response matches your request
            AccountKitLoginResult loginResult = data.getParcelableExtra(AccountKitLoginResult
                    .RESULT_KEY);
            String toastMessage;
            if (loginResult.getError() != null) {
                toastMessage = loginResult.getError().getErrorType().getMessage();
            } else if (loginResult.wasCancelled()) {
                toastMessage = "Login Cancelled";
            } else {
//                if (loginResult.getAccessToken() != null) {
//                    toastMessage = "Success:" + loginResult.getAccessToken().getAccountId();
//                } else {
//                    toastMessage = String.format(
//                            "Success:%s...",
//                            loginResult.getAuthorizationCode().substring(0, 10));
//                }
                netLine();
                Log.e("BasicActivity", loginResult.toString());
                // If you have an authorization code, retrieve it from
                // loginResult.getAuthorizationCode()
                // and pass it to your server and exchange it for an access token.

                // Success! Start your next activity...  成功后跳转下一个界面
//                goToMyLoggedInActivity();
            }
            // Surface the result to your user in an appropriate way.
        }
    }

    private void netLine() {
        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(final Account account) {
                String accountKitToken = AccountKit.getCurrentAccessToken().getToken();
                String accountKitId = account.getId();
                String accountKitPhone = account.getPhoneNumber() + "";
                Log.e("BasicActivity", "accountKitToken" + accountKitToken + ",accountKitId" +
                        accountKitId + "," + "accountKitPhone" + accountKitPhone);
                login(accountKitPhone, "000000");
            }

            @Override
            public void onError(final AccountKitError error) {
                Log.e("BasicActivity", "失败");
            }
        });
    }


    private void login(final String mobile, String code) {
        LoadingCustom.showprogress(mContext, "", true);
        OkHttpClient okHttpClient = new OkHttpClient();
        String url = Contants.LOGIN_ADDRESS;
////        Post请求
        final FormBody.Builder builder = new FormBody.Builder();
        builder.add("mobile", mobile);
        RequestBody formBody = builder.build();
        Request request1 = new Request.Builder().addHeader("X-SMS-CODE", code).addHeader
                ("Connection", "close").url(url).post(formBody).build();
        Call call1 = okHttpClient.newCall(request1);
        call1.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("result", "");
                message.setData(bundle);
                handlerBasic.sendMessage(message);
                Log.e("BasicActivity", e.toString());

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respones = response.body().string();
                Log.e("BasicActivity", respones);
                SharedPreUtil.saveSring(mContext, SharedPreUtil.PHONE, mobile);
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("result", respones);
                message.setData(bundle);
                handlerBasic.sendMessage(message);
            }
        });
    }
}
