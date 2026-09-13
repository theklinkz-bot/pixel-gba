"""Upload a release APK to a shared Google Drive folder."""
import json
import os
import sys

from google.oauth2 import credentials as oauth_credentials
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: upload_google_drive.py APK_PATH")
    apk_path = sys.argv[1]
    folder_id = os.environ["GOOGLE_DRIVE_FOLDER_ID"]
    refresh_token = os.environ.get("GOOGLE_DRIVE_REFRESH_TOKEN")
    if refresh_token:
        credentials = oauth_credentials.Credentials(
            token=None,
            refresh_token=refresh_token,
            token_uri="https://oauth2.googleapis.com/token",
            client_id=os.environ["GOOGLE_DRIVE_CLIENT_ID"],
            client_secret=os.environ["GOOGLE_DRIVE_CLIENT_SECRET"],
            scopes=["https://www.googleapis.com/auth/drive.file"],
        )
    else:
        credentials = service_account.Credentials.from_service_account_info(
            json.loads(os.environ["GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON"]),
            scopes=["https://www.googleapis.com/auth/drive.file"],
        )
    drive = build("drive", "v3", credentials=credentials, cache_discovery=False)
    metadata = {"name": os.path.basename(apk_path), "parents": [folder_id]}
    media = MediaFileUpload(apk_path, mimetype="application/vnd.android.package-archive", resumable=True)
    result = drive.files().create(body=metadata, media_body=media, fields="id,webViewLink").execute()
    print(f"Uploaded {apk_path} to Drive: {result.get('webViewLink', result['id'])}")


if __name__ == "__main__":
    main()
