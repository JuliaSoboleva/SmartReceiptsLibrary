package co.smartreceipts.android.sync.drive.rx;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.sync.drive.device.DeviceMetadata;
import co.smartreceipts.android.sync.drive.device.GoogleDriveSyncMetadata;
import co.smartreceipts.android.sync.drive.services.DriveIdUploadCompleteCallback;
import co.smartreceipts.android.sync.drive.services.DriveUploadCompleteManager;
import co.smartreceipts.android.sync.model.RemoteBackupMetadata;
import co.smartreceipts.android.sync.model.impl.DefaultRemoteBackupMetadata;
import co.smartreceipts.android.sync.model.impl.Identifier;
import co.smartreceipts.android.utils.UriUtils;
import rx.Observable;
import rx.Subscriber;
import wb.android.storage.StorageManager;

class DriveDataStreams {

    private static final String TAG = DriveDataStreams.class.getSimpleName();

    private static final String SMART_RECEIPTS_FOLDER = "Smart Receipts";
    private static final CustomPropertyKey SMART_RECEIPTS_FOLDER_KEY = new CustomPropertyKey("smart_receipts_id", CustomPropertyKey.PUBLIC);

    private final GoogleApiClient mGoogleApiClient;
    private final GoogleDriveSyncMetadata mGoogleDriveSyncMetadata;
    private final Context mContext;
    private final DeviceMetadata mDeviceMetadata;
    private final DriveUploadCompleteManager mDriveUploadCompleteManager;
    private final Executor mExecutor;

    public DriveDataStreams(@NonNull Context context, @NonNull GoogleApiClient googleApiClient, @NonNull GoogleDriveSyncMetadata googleDriveSyncMetadata) {
        this(googleApiClient, context, googleDriveSyncMetadata, new DeviceMetadata(context), new DriveUploadCompleteManager(), Executors.newCachedThreadPool());
    }

    public DriveDataStreams(@NonNull GoogleApiClient googleApiClient, @NonNull Context context, @NonNull GoogleDriveSyncMetadata googleDriveSyncMetadata,
                            @NonNull DeviceMetadata deviceMetadata, @NonNull DriveUploadCompleteManager driveUploadCompleteManager, @NonNull Executor executor) {
        mGoogleApiClient = Preconditions.checkNotNull(googleApiClient);
        mContext = Preconditions.checkNotNull(context.getApplicationContext());
        mGoogleDriveSyncMetadata = Preconditions.checkNotNull(googleDriveSyncMetadata);
        mDeviceMetadata = Preconditions.checkNotNull(deviceMetadata);
        mDriveUploadCompleteManager = Preconditions.checkNotNull(driveUploadCompleteManager);
        mExecutor = Preconditions.checkNotNull(executor);
    }

    public Observable<List<RemoteBackupMetadata>> getSmartReceiptsFolders() {
        return Observable.create(new Observable.OnSubscribe<List<RemoteBackupMetadata>>() {
            @Override
            public void call(final Subscriber<? super List<RemoteBackupMetadata>> subscriber) {
                final Query folderQuery = new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, SMART_RECEIPTS_FOLDER)).build();
                Drive.DriveApi.query(mGoogleApiClient, folderQuery).setResultCallback(new ResultCallbacks<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                        try {
                            final List<Metadata> folderMetadataList = new ArrayList<>();
                            for (final Metadata metadata : metadataBufferResult.getMetadataBuffer()) {
                                if (isValidSmartReceiptsFolder(metadata)) {
                                    folderMetadataList.add(metadata);
                                }
                            }

                            final AtomicInteger resultsCount = new AtomicInteger(folderMetadataList.size());
                            final List<RemoteBackupMetadata> resultsList = new ArrayList<>();
                            final Query databaseQuery = new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, DatabaseHelper.DATABASE_NAME)).build();
                            for (final Metadata metadata : folderMetadataList) {
                                final Identifier driveFolderId = new Identifier(metadata.getDriveId().getResourceId());
                                final Map<CustomPropertyKey, String> customPropertyMap = metadata.getCustomProperties();
                                if (customPropertyMap != null && customPropertyMap.containsKey(SMART_RECEIPTS_FOLDER_KEY)) {
                                    final Identifier syncDeviceIdentifier = new Identifier(customPropertyMap.get(SMART_RECEIPTS_FOLDER_KEY));
                                    final String deviceName = metadata.getDescription() != null ? metadata.getDescription() : "";
                                    final Date parentFolderLastModifiedDate = metadata.getModifiedDate();
                                    metadata.getDriveId().asDriveFolder().queryChildren(mGoogleApiClient, databaseQuery).setResultCallback(new ResultCallbacks<DriveApi.MetadataBufferResult>() {
                                        @Override
                                        public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                                            try {
                                                Date lastModifiedDate = parentFolderLastModifiedDate;
                                                for (final Metadata databaseMetadata : metadataBufferResult.getMetadataBuffer()) {
                                                    if (databaseMetadata.getModifiedDate().getTime() > lastModifiedDate.getTime()) {
                                                        lastModifiedDate = databaseMetadata.getModifiedDate();
                                                    }
                                                }
                                                resultsList.add(new DefaultRemoteBackupMetadata(driveFolderId, syncDeviceIdentifier, deviceName, lastModifiedDate));
                                            } finally {
                                                metadataBufferResult.getMetadataBuffer().release();
                                                if (resultsCount.decrementAndGet() == 0) {
                                                    subscriber.onNext(resultsList);
                                                    subscriber.onCompleted();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NonNull Status status) {
                                            Log.e(TAG, "Failed to query a database within the parent folder: " + status);
                                            subscriber.onError(new IOException(status.getStatusMessage()));
                                        }
                                    });
                                } else {
                                    Log.e(TAG, "Found an invalid Smart Receipts folder. Skipping");
                                }
                            }
                        } finally {
                            metadataBufferResult.getMetadataBuffer().release();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "Failed to query a Smart Receipts folder with status: " + status);
                        subscriber.onError(new IOException(status.getStatusMessage()));
                    }
                });
            }
        });
    }

    public Observable<DriveFolder> getSmartReceiptsFolder() {
        return Observable.create(new Observable.OnSubscribe<DriveFolder>() {
            @Override
            public void call(final Subscriber<? super DriveFolder> subscriber) {
                final Query folderQuery = new Query.Builder().addFilter(Filters.eq(SMART_RECEIPTS_FOLDER_KEY, mGoogleDriveSyncMetadata.getDeviceIdentifier().getId())).build();
                Drive.DriveApi.query(mGoogleApiClient, folderQuery).setResultCallback(new ResultCallbacks<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                        try {
                            DriveId folderId = null;
                            for (final Metadata metadata : metadataBufferResult.getMetadataBuffer()) {
                                if (isValidSmartReceiptsFolder(metadata)) {
                                    folderId = metadata.getDriveId();
                                    break;
                                }
                            }

                            if (folderId != null) {
                                Log.i(TAG, "Found an existing Google Drive folder for Smart Receipts");
                                subscriber.onNext(folderId.asDriveFolder());
                                subscriber.onCompleted();
                            } else {
                                Log.i(TAG, "Failed to find an existing Smart Receipts folder for this device. Creating a new one...");
                                final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(SMART_RECEIPTS_FOLDER).setDescription(mDeviceMetadata.getDeviceName()).setCustomProperty(SMART_RECEIPTS_FOLDER_KEY, mGoogleDriveSyncMetadata.getDeviceIdentifier().getId()).build();
                                Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(mGoogleApiClient, changeSet).setResultCallback(new ResultCallbacks<DriveFolder.DriveFolderResult>() {
                                    @Override
                                    public void onSuccess(@NonNull DriveFolder.DriveFolderResult driveFolderResult) {
                                        subscriber.onNext(driveFolderResult.getDriveFolder());
                                        subscriber.onCompleted();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Status status) {
                                        Log.e(TAG, "Failed to create a home folder with status: " + status);
                                        subscriber.onError(new IOException(status.getStatusMessage()));
                                    }
                                });
                            }
                        } finally {
                            metadataBufferResult.getMetadataBuffer().release();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "Failed to query a Smart Receipts folder with status: " + status);
                        subscriber.onError(new IOException(status.getStatusMessage()));
                    }
                });
            }
        });
    }

    public Observable<DriveFile> createFileInFolder(@NonNull final DriveFolder folder, @NonNull final File file) {
        Preconditions.checkNotNull(folder);
        Preconditions.checkNotNull(file);

        return Observable.create(new Observable.OnSubscribe<DriveFile>() {
            @Override
            public void call(final Subscriber<? super DriveFile> subscriber) {
                Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(new ResultCallbacks<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onSuccess(@NonNull final DriveApi.DriveContentsResult driveContentsResult) {
                        mExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                final DriveContents driveContents = driveContentsResult.getDriveContents();
                                OutputStream outputStream = null;
                                FileInputStream fileInputStream = null;
                                try {
                                    outputStream = driveContents.getOutputStream();
                                    fileInputStream = new FileInputStream(file);
                                    byte[] buffer = new byte[8192];
                                    int read;
                                    while ((read = fileInputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, read);
                                    }

                                    final Uri uri = Uri.fromFile(file);
                                    final String mimeType = UriUtils.getMimeType(uri, mContext.getContentResolver());
                                    final MetadataChangeSet.Builder builder = new MetadataChangeSet.Builder();
                                    builder.setTitle(file.getName());
                                    if (!TextUtils.isEmpty(mimeType)) {
                                        builder.setMimeType(mimeType);
                                    }
                                    final MetadataChangeSet changeSet = builder.build();
                                    folder.createFile(mGoogleApiClient, changeSet, driveContents, new ExecutionOptions.Builder().setNotifyOnCompletion(true).build()).setResultCallback(new ResultCallbacks<DriveFolder.DriveFileResult>() {
                                        @Override
                                        public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult) {
                                            final DriveFile driveFile = driveFileResult.getDriveFile();
                                            final DriveId driveFileId = driveFile.getDriveId();
                                            if (driveFileId.getResourceId() == null) {
                                                mDriveUploadCompleteManager.registerCallback(driveFileId, new DriveIdUploadCompleteCallback() {
                                                    @Override
                                                    public void onSuccess(@NonNull DriveId fetchedDriveId) {
                                                        subscriber.onNext(fetchedDriveId.asDriveFile());
                                                        subscriber.onCompleted();
                                                    }

                                                    @Override
                                                    public void onFailure(@NonNull DriveId driveId) {
                                                        subscriber.onError(new IOException("Failed to receive a Drive Id"));
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NonNull Status status) {
                                            Log.e(TAG, "Failed to create file with status: " + status);
                                            subscriber.onError(new IOException(status.getStatusMessage()));
                                        }
                                    });
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed write file with exception: ", e);
                                    subscriber.onError(e);
                                } finally {
                                    StorageManager.closeQuietly(fileInputStream);
                                    StorageManager.closeQuietly(outputStream);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "Failed to create file with status: " + status);
                        subscriber.onError(new IOException(status.getStatusMessage()));
                    }
                });
            }
        });
    }

    public Observable<DriveFile> updateFile(@NonNull final Identifier driveIdentifier, @NonNull final File file) {
        Preconditions.checkNotNull(driveIdentifier);
        Preconditions.checkNotNull(file);

        return Observable.create(new Observable.OnSubscribe<DriveFile>() {
            @Override
            public void call(final Subscriber<? super DriveFile> subscriber) {
                Drive.DriveApi.fetchDriveId(mGoogleApiClient, driveIdentifier.getId()).setResultCallback(new ResultCallbacks<DriveApi.DriveIdResult>() {
                    @Override
                    public void onSuccess(@NonNull DriveApi.DriveIdResult driveIdResult) {
                        final DriveId driveId = driveIdResult.getDriveId();
                        final DriveFile driveFile = driveId.asDriveFile();
                        driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).setResultCallback(new ResultCallbacks<DriveApi.DriveContentsResult>() {
                            @Override
                            public void onSuccess(@NonNull final DriveApi.DriveContentsResult driveContentsResult) {
                                mExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final DriveContents driveContents = driveContentsResult.getDriveContents();
                                        OutputStream outputStream = null;
                                        FileInputStream fileInputStream = null;
                                        try {
                                            outputStream = driveContents.getOutputStream();
                                            fileInputStream = new FileInputStream(file);
                                            byte[] buffer = new byte[8192];
                                            int read;
                                            while ((read = fileInputStream.read(buffer)) != -1) {
                                                outputStream.write(buffer, 0, read);
                                            }

                                            // TODO: Does the ID stay the same? Or do I need to monitor
                                            driveContents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallbacks<Status>() {
                                                @Override
                                                public void onSuccess(@NonNull Status status) {
                                                    subscriber.onNext(driveFile);
                                                    subscriber.onCompleted();
                                                }

                                                @Override
                                                public void onFailure(@NonNull Status status) {
                                                    Log.e(TAG, "Failed to updateDriveFile file with status: " + status);
                                                    subscriber.onError(new IOException(status.getStatusMessage()));
                                                }
                                            });
                                        } catch (IOException e) {
                                            Log.e(TAG, "Failed write file with exception: ", e);
                                            subscriber.onError(e);
                                        } finally {
                                            StorageManager.closeQuietly(fileInputStream);
                                            StorageManager.closeQuietly(outputStream);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Status status) {
                                Log.e(TAG, "Failed to updateDriveFile file with status: " + status);
                                subscriber.onError(new IOException(status.getStatusMessage()));
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "Failed to fetch drive id " + driveIdentifier + " to updateDriveFile with status: " + status);
                        subscriber.onError(new IOException(status.getStatusMessage()));
                    }
                });
            }
        });
    }

    public Observable<Boolean> deleteFile(@NonNull final Identifier driveIdentifier) {
        Preconditions.checkNotNull(driveIdentifier);

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                Drive.DriveApi.fetchDriveId(mGoogleApiClient, driveIdentifier.getId()).setResultCallback(new ResultCallbacks<DriveApi.DriveIdResult>() {
                    @Override
                    public void onSuccess(@NonNull DriveApi.DriveIdResult driveIdResult) {
                        final DriveId driveId = driveIdResult.getDriveId();
                        final DriveFile driveFile = driveId.asDriveFile();
                        driveFile.delete(mGoogleApiClient).setResultCallback(new ResultCallbacks<Status>() {
                            @Override
                            public void onSuccess(@NonNull Status status) {
                                Log.i(TAG, "Successfully deleted file with status: " + status);
                                subscriber.onNext(true);
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onFailure(@NonNull Status status) {
                                Log.e(TAG, "Failed to deleteDriveFile file with status: " + status);
                                subscriber.onNext(false);
                                subscriber.onCompleted();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "Failed to fetch drive id " + driveIdentifier + " to deleteDriveFile with status: " + status);
                        subscriber.onError(new IOException(status.getStatusMessage()));
                    }
                });
            }
        });
    }

    private boolean isValidSmartReceiptsFolder(@NonNull Metadata metadata) {
        return metadata.isFolder() && !metadata.isTrashed();
    }
}