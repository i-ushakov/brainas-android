package net.brainas.android.app.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/9/2015.
 */
public class Task {
    private int id;
    private int globalId;
    private String message = null;
    private String description = null;
    private boolean haveImage = false;
    private List<Condition> conditions = new ArrayList<>();

    public Task(String message) {
        this.message = message;
    }

    public Task(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public Task(int id, int globalId, String message) {
        this.id = id;
        this.globalId = globalId;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setGlobalId(int globalId) { this.globalId = globalId; }

    public int getGlobalId() { return this.globalId; }

    public int getExternalId() {
        return globalId;
    }

    public String getMessage() {
        return message;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setImage(boolean haveImage) {
        this.haveImage = haveImage;
    }

    public boolean haveImage() {
        return haveImage;
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }





}
