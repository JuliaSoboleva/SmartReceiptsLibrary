package co.smartreceipts.android.persistence.database.tables.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.sql.Date;
import java.util.Collections;

import co.smartreceipts.android.persistence.database.tables.AbstractSqlTable;
import co.smartreceipts.android.sync.SyncProvider;
import co.smartreceipts.android.sync.model.SyncState;
import co.smartreceipts.android.sync.model.impl.DefaultSyncState;
import co.smartreceipts.android.sync.model.impl.Identifier;
import co.smartreceipts.android.sync.model.impl.IdentifierMap;
import co.smartreceipts.android.sync.model.impl.MarkedForDeletionMap;
import co.smartreceipts.android.sync.model.impl.SyncStatusMap;

public class SyncStateAdapter {

    @NonNull
    public SyncState read(@NonNull Cursor cursor) {
        final int driveIdentifierIndex = cursor.getColumnIndex(AbstractSqlTable.COLUMN_DRIVE_SYNC_ID);
        final int driveIsSyncedIndex = cursor.getColumnIndex(AbstractSqlTable.COLUMN_DRIVE_IS_SYNCED);
        final int driveMarkedForDeletionIndex = cursor.getColumnIndex(AbstractSqlTable.COLUMN_DRIVE_MARKED_FOR_DELETION);
        final int lastLocalModificationTimeIndex = cursor.getColumnIndex(AbstractSqlTable.COLUMN_LAST_LOCAL_MODIFICATION_TIME);

        final String driveIdentifierString = cursor.getString(driveIdentifierIndex);
        final boolean driveIsSynced = cursor.getInt(driveIsSyncedIndex) > 0;
        final boolean driveMarkedForDeletion = cursor.getInt(driveMarkedForDeletionIndex) > 0;
        final long lastLocalModificationTimeLong = cursor.getLong(lastLocalModificationTimeIndex);

        final Date lastLocalModificationTime = new Date(lastLocalModificationTimeLong);

        if (driveIdentifierString != null) {
            final Identifier driveIdentifier = new Identifier(driveIdentifierString);
            final IdentifierMap identifierMap = new IdentifierMap(Collections.singletonMap(SyncProvider.GoogleDrive, driveIdentifier));
            final SyncStatusMap syncStatusMap = new SyncStatusMap(Collections.singletonMap(SyncProvider.GoogleDrive, driveIsSynced));
            final MarkedForDeletionMap markedForDeletionMap = new MarkedForDeletionMap(Collections.singletonMap(SyncProvider.GoogleDrive, driveMarkedForDeletion));
            return new DefaultSyncState(identifierMap, syncStatusMap, markedForDeletionMap, lastLocalModificationTime);
        } else {
            return new DefaultSyncState(lastLocalModificationTime);
        }
    }

    @NonNull
    public ContentValues write(@NonNull SyncState syncState) {
        final ContentValues values = new ContentValues();
        final Identifier driveIdentifier = syncState.getSyncId(SyncProvider.GoogleDrive);
        if (driveIdentifier != null) {
            values.put(AbstractSqlTable.COLUMN_DRIVE_SYNC_ID, driveIdentifier.getId());
            values.put(AbstractSqlTable.COLUMN_DRIVE_IS_SYNCED, syncState.isSynced(SyncProvider.GoogleDrive));
            values.put(AbstractSqlTable.COLUMN_DRIVE_MARKED_FOR_DELETION, syncState.isMarkedForDeletion(SyncProvider.GoogleDrive));
        }
        values.put(AbstractSqlTable.COLUMN_LAST_LOCAL_MODIFICATION_TIME, syncState.getLastLocalModificationTime().getTime());
        return values;
    }

}