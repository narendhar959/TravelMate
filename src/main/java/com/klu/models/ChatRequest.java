package com.klu.models;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    public String              message;
    public Integer             user_id;
    public boolean             payment_action;
    public Map<String, Object> user_profile;
    public List<Map<String, Object>> history;
    public Map<String, Object> booking_state;
}
