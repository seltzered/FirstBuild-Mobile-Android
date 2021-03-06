package com.firstbuild.androidapp.paragon;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firstbuild.androidapp.ParagonValues;
import com.firstbuild.androidapp.R;
import com.firstbuild.androidapp.paragon.datamodel.RecipeInfo;
import com.firstbuild.androidapp.paragon.datamodel.RecipeManager;
import com.firstbuild.androidapp.paragon.datamodel.StageInfo;
import com.firstbuild.androidapp.productmanager.ParagonInfo;
import com.firstbuild.androidapp.productmanager.ProductInfo;
import com.firstbuild.androidapp.productmanager.ProductManager;
import com.firstbuild.viewutil.gridCircleView;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class MultiStageStatusFragment extends Fragment {

    private String TAG = MultiStageStatusFragment.class.getSimpleName();

    private gridCircleView circle;
    private ImageView[] progressDots = new ImageView[4];
    private View layoutStatus;
    private ImageView imgStatus;
    private TextView textTempCurrent;
    private TextView textTempTarget;
    private TextView textStatusName;
    private TextView textLabelCurrent;
    private TextView textExplanation;
    private View btnNextStage;
    private View btnComplete;
    private View btnContinue;
    private MULTI_STAGE_COOK_STATE cookState = MULTI_STAGE_COOK_STATE.STATE_NONE;
    private ParagonMainActivity attached = null;

    public enum MULTI_STAGE_COOK_STATE {
        STATE_NONE,
        STATE_HEAT_COOL,
        STATE_TARGET_TEMP_REACHED,
        STATE_HOLDING_TEMP,
        STATE_NEXT_STAGE,
        STATE_DONE,
    }

    public MultiStageStatusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        attached = (ParagonMainActivity)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        attached.setTitle("Multi Stage");

        View view = inflater.inflate(R.layout.fragment_multi_stage_status, container, false);

        circle = (gridCircleView) view.findViewById(R.id.circle);
        circle.setBarValue(0);
        circle.setGridValue(0);
        circle.setDashValue(0);

        textStatusName = (TextView) view.findViewById(R.id.text_status_name);
        textLabelCurrent = (TextView) view.findViewById(R.id.text_label_current);

        progressDots[0] = (ImageView) view.findViewById(R.id.progress_dot_1);
        progressDots[1] = (ImageView) view.findViewById(R.id.progress_dot_2);
        progressDots[2] = (ImageView) view.findViewById(R.id.progress_dot_3);
        view.findViewById(R.id.progress_dot_4).setVisibility(View.GONE);

        textTempCurrent = (TextView) view.findViewById(R.id.text_temp_current);
        textTempTarget = (TextView) view.findViewById(R.id.text_temp_target);
        textExplanation = (TextView) view.findViewById(R.id.text_explanation);

        textExplanation.setVisibility(View.GONE);

        layoutStatus = view.findViewById(R.id.layout_status);
        imgStatus = (ImageView) view.findViewById(R.id.img_status);

        layoutStatus.setVisibility(View.VISIBLE);
        imgStatus.setVisibility(View.GONE);

        btnContinue = view.findViewById(R.id.btn_continue);
        btnContinue.setVisibility(View.GONE);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ByteBuffer valueBuffer = ByteBuffer.allocate(1);

                valueBuffer.put((byte) 0x01);
//                BleManager.getInstance().writeCharacteristics(ParagonValues.CHARACTERISTIC_START_HOLD_TIMER, valueBuffer.array());

                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_HOLDING_TEMP);

            }
        });


        btnNextStage = view.findViewById(R.id.btn_next_stage);
        btnNextStage.setVisibility(View.GONE);
        btnNextStage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nextStage = RecipeManager.getInstance().getCurrentStageIndex() + 1;

                ByteBuffer valueBuffer = ByteBuffer.allocate(1);

                valueBuffer.put((byte) nextStage);
//                BleManager.getInstance().writeCharacteristics(ParagonValues.CHARACTERISTIC_CURRENT_COOK_STAGE, valueBuffer.array());

                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_HEAT_COOL);
            }
        });

        btnComplete = view.findViewById(R.id.btn_complete);
        btnComplete.setVisibility(View.GONE);
        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ParagonMainActivity) getActivity()).nextStep(ParagonMainActivity.ParagonSteps.STEP_SOUSVIDE_COMPLETE);
            }
        });

        updateUiCurrentTemp();

        UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_HEAT_COOL);

        return view;
    }


    /**
     * Update cooking state, Off -> Heating -> Ready -> Cooking -> Done
     *
     */
    public void updateCookState() {
        ParagonInfo productInfo = (ParagonInfo)ProductManager.getInstance().getCurrent();
        byte state = productInfo.getErdCookState();
        Log.d(TAG, "updateCookState IN " + state);

        switch (state) {
            case ParagonValues.COOK_STATE_OFF:
                attached.nextStep(ParagonMainActivity.ParagonSteps.STEP_COOKING_MODE);
                break;

            case ParagonValues.COOK_STATE_HEATING:
                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_HEAT_COOL);
                break;

            case ParagonValues.COOK_STATE_READY:
                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_TARGET_TEMP_REACHED);
                break;

            case ParagonValues.COOK_STATE_COOKING:
                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_HOLDING_TEMP);
                break;

            case ParagonValues.COOK_STATE_DONE:
                UpdateUiCookState(MULTI_STAGE_COOK_STATE.STATE_DONE);
                break;

            default:
                Log.d(TAG, "Error in onCookState :" + state);
                break;
        }

    }


    /**
     * Update UI progress bar top of the screen and on circle view.
     */
    public void UpdateUiCookState(MULTI_STAGE_COOK_STATE state) {
        Log.d(TAG, "UpdateUiCookState " + state);

        RecipeInfo recipeInfo = RecipeManager.getInstance().getCurrentRecipe();
        StageInfo stageInfo = RecipeManager.getInstance().getCurrentStage();
        int currentStageIndex = RecipeManager.getInstance().getCurrentStageIndex();

        if (cookState != state) {
            cookState = state;

            if(currentStageIndex == 0){
                // show the 'Ready to Cook' image only for first stage.
                progressDots[1].setVisibility(View.VISIBLE);
            }
            else{
                //other than hide the image.
                progressDots[1].setVisibility(View.GONE);
            }

            switch (cookState) {
                case STATE_HEAT_COOL:
                    textStatusName.setText("");
                    progressDots[0].setImageResource(R.drawable.ic_step_dot_current);
                    progressDots[1].setImageResource(R.drawable.ic_step_dot_todo);
                    progressDots[2].setImageResource(R.drawable.ic_step_dot_todo);

                    layoutStatus.setVisibility(View.VISIBLE);
                    imgStatus.setVisibility(View.GONE);

                    btnContinue.setVisibility(View.GONE);
                    btnNextStage.setVisibility(View.GONE);
                    btnComplete.setVisibility(View.GONE);

                    textTempTarget.setText(Html.fromHtml("Target: " + stageInfo.getTemp() + "<small>℉</small>"));
                    textLabelCurrent.setText("Current:");
                    textTempCurrent.setText("");
                    textExplanation.setText(recipeInfo.getDirections());

                    circle.setBarValue(0);
                    break;

                case STATE_TARGET_TEMP_REACHED:
                    textStatusName.setText("TARGET TEMPERATURE REACHED");
                    progressDots[0].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[1].setImageResource(R.drawable.ic_step_dot_current);
                    progressDots[2].setImageResource(R.drawable.ic_step_dot_todo);

                    layoutStatus.setVisibility(View.GONE);
                    imgStatus.setVisibility(View.VISIBLE);

                    btnContinue.setVisibility(View.VISIBLE);
                    btnNextStage.setVisibility(View.GONE);
                    btnComplete.setVisibility(View.GONE);

                    textTempTarget.setText(Html.fromHtml("Target: " + stageInfo.getTemp() + "<small>℉</small>"));

                    imgStatus.setImageResource(R.drawable.img_ready_to_cook);

                    circle.setGridValue(1.0f);
                    circle.setBarValue(0);

                    textExplanation.setText(recipeInfo.getDirections());
                    break;

                case STATE_HOLDING_TEMP:
                    textStatusName.setText("HOLDING TEMPERATURE");
                    progressDots[0].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[1].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[2].setImageResource(R.drawable.ic_step_dot_current);

                    layoutStatus.setVisibility(View.VISIBLE);
                    imgStatus.setVisibility(View.GONE);

                    btnContinue.setVisibility(View.GONE);
                    btnNextStage.setVisibility(View.GONE);
                    btnComplete.setVisibility(View.GONE);

                    textTempTarget.setText(Html.fromHtml(stageInfo.getTemp() + "<small>℉</small>"));
                    textLabelCurrent.setText("Food ready at");
                    textTempCurrent.setText("");

                    circle.setGridValue(1.0f);

                    textExplanation.setText(recipeInfo.getDirections());
                    break;

                case STATE_NEXT_STAGE:
                    textStatusName.setText("HOLDING TEMPERATURE");
                    progressDots[0].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[1].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[2].setImageResource(R.drawable.ic_step_dot_current);

                    layoutStatus.setVisibility(View.VISIBLE);
                    imgStatus.setVisibility(View.GONE);

                    btnContinue.setVisibility(View.GONE);
                    btnNextStage.setVisibility(View.VISIBLE);
                    btnComplete.setVisibility(View.GONE);

                    textTempTarget.setText(Html.fromHtml(stageInfo.getTemp() + "<small>℉</small>"));
                    textLabelCurrent.setText("Food ready at");
                    textTempCurrent.setText("");

                    circle.setGridValue(1.0f);

                    textExplanation.setText(recipeInfo.getDirections());
                    break;

                case STATE_DONE:
                    textStatusName.setText("HOLDING TEMPERATURE");
                    progressDots[0].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[1].setImageResource(R.drawable.ic_step_dot_done);
                    progressDots[2].setImageResource(R.drawable.ic_step_dot_current);

                    layoutStatus.setVisibility(View.VISIBLE);
                    imgStatus.setVisibility(View.GONE);

                    btnContinue.setVisibility(View.GONE);
                    btnNextStage.setVisibility(View.GONE);
                    btnComplete.setVisibility(View.VISIBLE);

                    textTempTarget.setText(Html.fromHtml(stageInfo.getTemp() + "<small>℉</small>"));
                    textLabelCurrent.setText("Food is");
                    textTempCurrent.setText("READY");

                    circle.setGridValue(1.0f);

                    textExplanation.setVisibility(View.VISIBLE);
                    textExplanation.setText(R.string.fragment_soudvide_status_explanation_donekeep);
                    break;

                default:
                    break;
            }

        }
        else {
            //do nothing.
        }


    }


    /**
     * Update UI current temperature compare with set temperature.r
     */
    public void updateUiCurrentTemp() {
        StageInfo stageInfo = RecipeManager.getInstance().getCurrentStage();

        if (cookState == MULTI_STAGE_COOK_STATE.STATE_HEAT_COOL) {

            ParagonInfo productInfo = (ParagonInfo)ProductManager.getInstance().getCurrent();
            int currentTemp = Math.round(productInfo.getErdCurrentTemp());
            int targetTep = stageInfo.getTemp();

            if(currentTemp < targetTep){
                textStatusName.setText("HEATING");
            }
            else{
                textStatusName.setText("COOLING");
            }

            textTempCurrent.setText(currentTemp + "℉");

            float ratioTemp = currentTemp / (float)stageInfo.getTemp();

            ratioTemp = Math.min(ratioTemp, 1.0f);
            circle.setGridValue(ratioTemp);
        }
        else {
            //do nothing.
        }
    }


    /**
     * Update UI current elapsed time. Elapsed time is increase from 0 until selected cook time.
     *
     */
    public void updateUiElapsedTime() {
        ParagonInfo productInfo = (ParagonInfo)ProductManager.getInstance().getCurrent();
        int elapsedTime = productInfo.getErdElapsedTime();

        Log.d(TAG, "updateUiElapsedTime :" + elapsedTime);
        StageInfo stageInfo = RecipeManager.getInstance().getCurrentStage();

        if (cookState == MULTI_STAGE_COOK_STATE.STATE_HOLDING_TEMP) {
            float ratioTime = (float) elapsedTime / (float) stageInfo.getTime();

            ratioTime = Math.min(ratioTime, 1.0f);

            circle.setBarValue(1.0f - ratioTime);

            updateReadyTime(elapsedTime);
        }
        else {
            //do nothing
        }
    }


    /**
     * Udate new stage get from BLE master.
     */
    public void updateCookStage() {
        ParagonInfo productInfo = (ParagonInfo)ProductManager.getInstance().getCurrent();
        int newStage = productInfo.getErdCookStage();
        RecipeManager.getInstance().setCurrentStage(newStage-1);

        attached.setTitle("Stage "+newStage);
    }


    private void updateReadyTime(int minute){
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, minute);
        String timeText = String.format( "%d:%02d", now.get(Calendar.HOUR), now.get(Calendar.MINUTE));
        String ampm = "";

        if(now.get(Calendar.AM_PM) == Calendar.AM){
            ampm = "AM";
        }
        else{
            ampm = "PM";
        }

        textTempCurrent.setText(Html.fromHtml(timeText + "<small>"+ampm+"</small>"));
    }

}
