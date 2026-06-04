package util;

import model.TripPlan;

import java.util.Stack;

/*
  UndoManager sınıfı, What-If simülasyonu sırasında yapılan değişiklikleri
  geri alabilmek için Stack veri yapısını kullanır.

  Kullanıcı bir lokasyon eklediğinde veya çıkardığında, mevcut rota önce
  stack'e kaydedilir. Kullanıcı "Geri Al" dediğinde son rota stack'ten
  alınarak tekrar kullanılabilir.
 */
public class UndoManager {

    private Stack<TripPlan> undoStack;

    public UndoManager() {
        undoStack = new Stack<>();
    }

    public void push(TripPlan plan) {
        if (plan != null) {
            undoStack.push(plan);
        }
    }

    public TripPlan undo() {
        if (undoStack.isEmpty()) {
            return null;
        }

        return undoStack.pop();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public int size() {
        return undoStack.size();
    }

    public void clear() {
        undoStack.clear();
    }
}