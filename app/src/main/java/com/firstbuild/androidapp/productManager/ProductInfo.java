package com.firstbuild.androidapp.productManager;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.firstbuild.androidapp.ParagonValues;
import com.firstbuild.androidapp.paragon.dataModel.RecipeInfo;
import com.firstbuild.androidapp.paragon.dataModel.StageInfo;
import com.firstbuild.commonframework.bleManager.BleManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

public class ProductInfo {

    public static final int PRODUCT_TYPE_CILLHUB = 0;
    public static final int PRODUCT_TYPE_PARAGON = 1;
    public static final int NO_BATTERY_INFO = -1;

    public static final byte INITIAL_VALUE = 0x0f;
    private static String TAG = ProductInfo.class.getSimpleName();
    //type can be Paragon and Chilhub so far.
    public int type = -1;
    public String address = "";
    public String nickname = "";
    public BluetoothDevice bluetoothDevice = null;
    //properties get from device.
    private boolean isConnected = false;
    private byte erdProbeConnectionStatue;
    private int erdBatteryLevel = NO_BATTERY_INFO;
    private float erdCurrentTemp;
    private int erdElapsedTime;
    private RecipeInfo erdRecipeConfig = null;
    private byte erdBurnerStatus = INITIAL_VALUE;
    private byte erdCookState;
    private byte erdCookStage;
    private byte erdCurrentCookMode = INITIAL_VALUE;


    public ProductInfo(int type, String address, String nickname) {
        this.address = address;
        this.nickname = nickname;
        this.type = type;
        this.erdBatteryLevel = -1;
        this.isConnected = false;
    }

    public ProductInfo(ProductInfo product) {
        this.type = product.type;
        this.address = product.address;
        this.nickname = product.nickname;
        this.erdBatteryLevel = -1;
        this.isConnected = false;
    }

    public ProductInfo(JSONObject jsonObject) {

        try {
            this.type = jsonObject.getInt("type");
        }
        catch (JSONException e) {
            this.type = PRODUCT_TYPE_PARAGON;
        }


        try {
            this.nickname = jsonObject.getString("nickname");

        }
        catch (JSONException e) {
            this.nickname = "";
        }


        try {
            this.address = jsonObject.getString("address");

        }
        catch (JSONException e) {
            this.address = "";
        }

    }


    public JSONObject toJson() {

        JSONObject object = new JSONObject();

        try {
            object.put("type", type);
            object.put("nickname", nickname);
            object.put("address", address);

        }
        catch (JSONException e) {
            //let content has ""
        }

        return object;
    }


    public void connected() {
        if (isConnected == false) {
            isConnected = true;
        }
    }

    public void disconnected() {
        if (isConnected == true) {
            isConnected = false;
            erdProbeConnectionStatue = ParagonValues.PROBE_NOT_CONNECT;
            erdBatteryLevel = NO_BATTERY_INFO;
            erdCurrentTemp = 0.0f;
            erdElapsedTime = 0;
            erdRecipeConfig = null;
            erdBurnerStatus = INITIAL_VALUE;
            erdCookState = INITIAL_VALUE;
            erdCookStage = INITIAL_VALUE;
            erdCurrentCookMode = INITIAL_VALUE;
        }
    }

    public void sendRecipeConfig() {
        ByteBuffer valueBuffer = ByteBuffer.allocate(40);

        int numStage = erdRecipeConfig.numStage();

        for (int i = 0; i < numStage; i++) {
            StageInfo stage = erdRecipeConfig.getStage(i);

            valueBuffer.put(8 * i, (byte) stage.getSpeed());
            valueBuffer.putShort(1 + 8 * i, (short) (stage.getTime()));
            valueBuffer.putShort(3 + 8 * i, (short) (stage.getMaxTime()));
            valueBuffer.putShort(5 + 8 * i, (short) (stage.getTemp() * 100));
            valueBuffer.put(7 + 8 * i, (byte) (stage.isAutoTransition() ? 0x01 : 0x02));
        }

        for (int i = 0; i < 40; i++) {
            Log.d(TAG, "RecipeManager.sendCurrentStages:" + String.format("0x%02x", valueBuffer.array()[i]));
        }

        BleManager.getInstance().writeCharateristics(bluetoothDevice, ParagonValues.CHARACTERISTIC_COOK_CONFIGURATION, valueBuffer.array());
    }


    public void createRecipeConfigForSousVide() {
        this.erdRecipeConfig = new RecipeInfo("", "", "", "");
        this.erdRecipeConfig.setType(RecipeInfo.TYPE_SOUSVIDE);
        this.erdRecipeConfig.addStage(new StageInfo());
    }



    public boolean isConnected() {
        return isConnected;
    }


    public float getErdCurrentTemp() {
        return erdCurrentTemp;
    }

    public void setErdCurrentTemp(short erdCurrentTemp) {
        this.erdCurrentTemp = (erdCurrentTemp / 100.0f);
    }

    public int getErdElapsedTime() {
        return erdElapsedTime;
    }

    public void setErdElapsedTime(int erdRemainingTime) {
        this.erdElapsedTime = erdRemainingTime;
    }

    public RecipeInfo getErdRecipeConfig() {
        return erdRecipeConfig;
    }

    public void setErdRecipeConfig(RecipeInfo erdRecipeConfig) {
        this.erdRecipeConfig = erdRecipeConfig;
    }

    public byte getErdBurnerStatus() {
        return erdBurnerStatus;
    }

    public void setErdBurnerStatus(byte erdBurnerStatus) {
        this.erdBurnerStatus = erdBurnerStatus;
    }

    public byte getErdCookState() {
        return erdCookState;
    }

    public void setErdCookState(byte erdCookState) {
        this.erdCookState = erdCookState;
    }

    public byte getErdCookStage() {
        return erdCookStage;
    }

    public void setErdCookStage(byte erdCookStage) {
        this.erdCookStage = erdCookStage;
    }

    public byte getErdCurrentCookMode() {
        return erdCurrentCookMode;
    }

    public void setErdCurrentCookMode(byte erdCurrentCookMode) {
        this.erdCurrentCookMode = erdCurrentCookMode;
    }

    public int getErdBatteryLevel() {
        return erdBatteryLevel;
    }

    public void setErdBatteryLevel(int erdBatteryLevel) {
        this.erdBatteryLevel = erdBatteryLevel;
    }

    public byte getErdProbeConnectionStatue() {
        return erdProbeConnectionStatue;
    }

    public void setErdProbeConnectionStatue(byte erdProbeConnectionStatue) {
        this.erdProbeConnectionStatue = erdProbeConnectionStatue;
    }

    public boolean isProbeConnected() {
        return (this.erdProbeConnectionStatue == ParagonValues.PROBE_CONNECT);
    }
}
