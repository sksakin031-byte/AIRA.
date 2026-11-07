# AIRA Android wrapper

এই directory-তে Android UI ও Python bridge আছে। মূল `AIRA-SOURCE-FINAL.zip` অপরিবর্তিত থাকে: GitHub Actions archive-এর SHA যাচাই করে, runner-এর অস্থায়ী directory-তে extract করে, upstream tests চালায় এবং ZIP-এর Python modules-কে Chaquopy app build input হিসেবে কপি করে। ZIP file-এ কোনো edit, repack বা re-save হয় না।

## APK ডাউনলোড

`main`-এ wrapper/workflow পরিবর্তন বা source ZIP পরিবর্তিত হলে Actions build স্বয়ংক্রিয়ভাবে শুরু হয়। হাতে চালাতে GitHub repository-র **Actions → Build AIRA APK from source ZIP → Run workflow** ব্যবহার করুন। সফল run-এর `AIRA-Android-debug-<run>` artifact-এ APK ও `SHA256SUMS` থাকে; artifact ৩০ দিন রাখা হয়।

## অ্যাপ ব্যবহার

অ্যাপ চালু করে **API key** button-এ নিজের Gemini API key দিন। Keyটি Android app-এর private storage-এ সংরক্ষিত হয়; APK বা repository-তে থাকে না। AI request সরাসরি Google Gemini API-তে যায়, তাই chat-এর জন্য internet connection ও নিজের API key দরকার।

Wrapper upstream AIRA Python engine ব্যবহার করে। নিরাপত্তার জন্য approval-চাওয়া tool action-এ এটি স্বয়ংক্রিয়ভাবে “না” বলে। Terminal-এর multiline `/write` ও `/overwrite` command wrapper UI-তে নিষ্ক্রিয়; Android wrapper থেকে source ZIP বা project files লেখা হয় না।

## Build configuration

Chaquopy Python 3.13 runtime bundle করে। APK `arm64-v8a` Android phone-এ (যেমন Redmi) এবং `x86_64` emulator-এ কাজ করার জন্য build হয়। Minimum Android API 24। CI debug-signed APK দেয়; এটি testing-এর জন্য, Play Store release-এর জন্য নয়।
