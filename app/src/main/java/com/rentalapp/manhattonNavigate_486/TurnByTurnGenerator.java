
package com.rentalapp.manhattonNavigate_486;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.rentalapp.InDevelop.AccessibleRouteFinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class TurnByTurnGenerator {

    private static final String TAG = "TurnByTurnGenerator";
    private static final String OPENAI_API_KEY = "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";
    private final RequestQueue queue;

    public interface OnInstructionsReady {
        void onGenerated(String instructionsText);
    }

    public TurnByTurnGenerator(@NonNull android.content.Context context) {
        this.queue = Volley.newRequestQueue(context);
    }

    public void generateInstructions(List<AccessibleRouteFinder.RouteStep> steps, OnInstructionsReady callback) {
        try {
            JSONArray jsonSteps = new JSONArray();
            for (int i = 0; i < steps.size(); i++) {
                var step = steps.get(i);
                JSONObject obj = new JSONObject();
                obj.put("index", i + 1);
                obj.put("instruction", step.instruction);
                obj.put("distance", step.distance);
                obj.put("obstacle", step.hasObstacle ? "yes" : "no");
                jsonSteps.put(obj);
            }

            JSONObject payload = new JSONObject();
            payload.put("model", "gpt-4");
            JSONArray messages = new JSONArray();

            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "你是一个无障碍导航助手，请将以下路线信息生成一段轮椅用户专用的逐步导航指令，避免楼梯或危险区域，尽量友好自然。"));

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "以下是路径步骤：" + jsonSteps.toString()));

            payload.put("messages", messages);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    "https://api.openai.com/v1/chat/completions",
                    payload,
                    response -> {
                        try {
                            String content = response
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            callback.onGenerated(content);
                            Log.d(TAG, "✅ GPT 输出：" + content);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ 解析 GPT 响应失败", e);
                        }
                    },
                    error -> Log.e(TAG, "❌ GPT 请求失败", error)
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Authorization", "Bearer " + OPENAI_API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            Log.e(TAG, "❌ 构建 OpenAI 请求失败", e);
        }
    }
}
