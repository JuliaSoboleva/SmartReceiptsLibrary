package co.smartreceipts.android.sync.provider;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import co.smartreceipts.android.sync.BackupProvider;
import co.smartreceipts.android.sync.noop.NoOpBackupProvider;

public class SyncProviderFactory {

    @Inject
    public SyncProviderFactory() {
    }

    public BackupProvider get(@NonNull SyncProvider syncProvider) {
        return new NoOpBackupProvider();
    }
}
