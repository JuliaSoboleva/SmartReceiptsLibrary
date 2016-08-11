package co.smartreceipts.android.persistence.database.controllers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

public interface TableEventsListener<T> {

    void onGetSuccess(@NonNull List<T> list);

    void onGetFailure(@Nullable Throwable e);

    void onInsertSuccess(@NonNull T t);

    void onInsertFailure(@NonNull T t, @Nullable Throwable e);

    void onUpdateSuccess(@NonNull T oldT, @NonNull T newT);

    void onUpdateFailure(@NonNull T oldT, @Nullable Throwable e);

    void onDeleteSuccess(@NonNull T t);

    void onDeleteFailure(@NonNull T t, @Nullable Throwable e);

}