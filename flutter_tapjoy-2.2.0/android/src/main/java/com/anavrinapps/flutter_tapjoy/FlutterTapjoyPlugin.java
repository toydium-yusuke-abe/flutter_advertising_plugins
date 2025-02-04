package com.anavrinapps.flutter_tapjoy;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.tapjoy.TJActionRequest;
import com.tapjoy.TJAwardCurrencyListener;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJEarnedCurrencyListener;
import com.tapjoy.TJError;
import com.tapjoy.TJGetCurrencyBalanceListener;
import com.tapjoy.TJSetUserIDListener;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJSpendCurrencyListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyConnectFlag;

import java.util.Hashtable;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FlutterTapjoyPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    private Activity activity;
    private Context context;
    private Hashtable<String, TJPlacement> placements = new Hashtable<>();

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_tapjoy");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull final Result result) {
        switch (call.method) {
            case "connectTapJoy":
                final String tapjoySDKKey = call.argument("androidApiKey");
                final Boolean debug = call.argument("debug");
                Hashtable<String, Object> connectFlags = new Hashtable<>();
                if (debug) {
                    connectFlags.put(TapjoyConnectFlag.ENABLE_LOGGING, "true");
                }
                Tapjoy.setDebugEnabled(debug);
                Tapjoy.connect(context, tapjoySDKKey, connectFlags, new TJConnectListener() {
                    @Override
                    public void onConnectSuccess() {
                        channel.invokeMethod("connectionSuccess", null);
                    }

                    @Override
                    public void onConnectFailure(int code, String errorMessage) {
                        Hashtable<String, Object> error = new Hashtable<>();
                        error.put("code", code);
                        error.put("error", errorMessage);
                        channel.invokeMethod("connectionFail", error);
                    }

                    @Override
                    public void onConnectWarning(int code, String errorMessage) {
                        Hashtable<String, Object> error = new Hashtable<>();
                        error.put("code", code);
                        error.put("error", errorMessage);
                        channel.invokeMethod("connectionWarning", error);
                    }
                });
                result.success(Tapjoy.isConnected());
                Tapjoy.setEarnedCurrencyListener(new TJEarnedCurrencyListener() {
                    @Override
                    public void onEarnedCurrency(String currencyName, int amount) {
                        Hashtable<String, Object> getCurrencyResponse = new Hashtable<>();
                        getCurrencyResponse.put("currencyName", currencyName);
                        getCurrencyResponse.put("earnedAmount", amount);
                        invokeMethod("onEarnedCurrency", getCurrencyResponse, result);
                    }
                });
                break;
            case "setUserID":
                final String userID = call.argument("userID");

                Tapjoy.setUserID(userID, new TJSetUserIDListener() {
                    @Override
                    public void onSetUserIDSuccess() {
                        final Hashtable<String, Object> retValue = new Hashtable<>();
                        retValue.put("error", "");
                        invokeMethod("setUserIdResult", retValue, result);
                    }

                    @Override
                    public void onSetUserIDFailure(String error) {
                        final Hashtable<String, Object> retValue = new Hashtable<>();
                        retValue.put("error", error);
                        invokeMethod("setUserIdResult", retValue, result);
                    }
                });
                break;
            case "isConnected":
                result.success(Tapjoy.isConnected());
                break;
            case "createPlacement":
                final String placementName = call.argument("placementName");
                TJPlacementListener placementListener = new TJPlacementListener() {
                    @Override
                    public void onRequestSuccess(final TJPlacement tjPlacement) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        invokeMethod("requestSuccess", myMap, result);
                    }

                    @Override
                    public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        myMap.put("error", tjError.message);
                        invokeMethod("requestFail", myMap, result);
                    }

                    @Override
                    public void onContentReady(TJPlacement tjPlacement) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        invokeMethod("contentReady", myMap, result);
                    }

                    @Override
                    public void onContentShow(TJPlacement tjPlacement) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        invokeMethod("contentDidAppear", myMap, result);
                    }

                    @Override
                    public void onContentDismiss(TJPlacement tjPlacement) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        invokeMethod("contentDidDisAppear", myMap, result);
                    }

                    @Override
                    public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {
                    }

                    @Override
                    public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {
                    }

                    @Override
                    public void onClick(TJPlacement tjPlacement) {
                        final Hashtable<String, Object> myMap = new Hashtable<>();
                        myMap.put("placementName", tjPlacement.getName());
                        invokeMethod("clicked", myMap, result);
                    }
                };
                TJPlacement p = Tapjoy.getPlacement(placementName, placementListener);
                placements.put(placementName, p);
                result.success(p.isContentAvailable());
                break;
            case "requestContent":
                final String placementNameRequest = call.argument("placementName");
                final TJPlacement tjPlacementRequest = placements.get(placementNameRequest);
                if (tjPlacementRequest != null) {
                    try {
                        tjPlacementRequest.requestContent();
                    }
                    catch (final Exception e) {
                        result.error("Error", e.getMessage(), null);
                    }
                } else {
                    final Hashtable<String, Object> myMap = new Hashtable<>();
                    myMap.put("placementName", placementNameRequest);
                    myMap.put("error", "Placement Not Found, Please Add placement first");
                    invokeMethod("requestFail", myMap, result);
                }
                break;
            case "showPlacement":
                try {
                    final String placementNameShow = call.argument("placementName");
                    final TJPlacement tjPlacementShow = placements.get(placementNameShow);
                    assert tjPlacementShow != null;
                    tjPlacementShow.showContent();
                }
                catch (final Exception e) {
                    result.error("Error", e.getMessage(), null);
                }
                break;
            case "getCurrencyBalance":
                Tapjoy.getCurrencyBalance(new TJGetCurrencyBalanceListener() {
                    @Override
                    public void onGetCurrencyBalanceResponse(String currencyName, int balance) {
                        Hashtable<String, Object> getCurrencyResponse = new Hashtable<>();
                        getCurrencyResponse.put("currencyName", currencyName);
                        getCurrencyResponse.put("balance", balance);
                        invokeMethod("onGetCurrencyBalanceResponse", getCurrencyResponse, result);
                    }

                    @Override
                    public void onGetCurrencyBalanceResponseFailure(String error) {
                        Hashtable<String, Object> getCurrencyResponse = new Hashtable<>();
                        getCurrencyResponse.put("error", error);
                        invokeMethod("onGetCurrencyBalanceResponse", getCurrencyResponse, result);
                    }
                });
                break;
            case "spendCurrency":
                final int myAmountInt = call.argument("amount");
                Tapjoy.spendCurrency(myAmountInt, new TJSpendCurrencyListener() {
                    @Override
                    public void onSpendCurrencyResponse(String currencyName, int balance) {
                        Hashtable<String, Object> spendCurrencyResponse = new Hashtable<>();
                        spendCurrencyResponse.put("currencyName", currencyName);
                        spendCurrencyResponse.put("balance", balance);
                        invokeMethod("onSpendCurrencyResponse", spendCurrencyResponse, result);
                    }

                    @Override
                    public void onSpendCurrencyResponseFailure(String error) {
                        Hashtable<String, Object> spendCurrencyResponse = new Hashtable<>();
                        spendCurrencyResponse.put("error", error);
                        invokeMethod("onSpendCurrencyResponse", spendCurrencyResponse, result);
                    }
                });
                break;
            case "awardCurrency":
                final int myAmountIntAward = call.argument("amount");
                Tapjoy.awardCurrency(myAmountIntAward, new TJAwardCurrencyListener() {
                    @Override
                    public void onAwardCurrencyResponse(String currencyName, int balance) {
                        Hashtable<String, Object> awardCurrencyResponse = new Hashtable<>();
                        awardCurrencyResponse.put("currencyName", currencyName);
                        awardCurrencyResponse.put("balance", balance);
                        invokeMethod("onAwardCurrencyResponse", awardCurrencyResponse, result);
                    }

                    @Override
                    public void onAwardCurrencyResponseFailure(String error) {
                        Hashtable<String, Object> awardCurrencyResponse = new Hashtable<>();
                        awardCurrencyResponse.put("error", error);
                        invokeMethod("onAwardCurrencyResponse", awardCurrencyResponse, result);
                    }
                });
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
    }

    private void invokeMethod(@NonNull final String methodName, final Hashtable<String, Object> data, Result result) {
        try {
            activity.runOnUiThread(() -> channel.invokeMethod(methodName, data));
        }
        catch(final Exception e) {
            result.error("Error", e.getMessage(), null);
        }
    }
}

