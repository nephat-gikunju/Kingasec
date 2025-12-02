# KingaSec: Privacy Guardian Application 🛡️

## 🎯 Project Overview
**KingaSec** (a blend of Swahili's "Kinga" - to protect/shield, and "Security") is an intelligent Android application developed to combat privacy invasion by predatory mobile applications, particularly digital lending (loan) apps common in Kenya.

The application uses a custom-trained **Machine Learning model** deployed via **TensorFlow Lite (TFLite)** to analyze installed applications, assess their permission usage, and predict a privacy risk score in real-time.

---

## 💡 Problem Statement
In the Kenyan market, many digital credit provider apps have been widely reported for:
* **Unauthorized access** to personal contacts and SMS messages.
* **Privacy invasion** leading to damaged personal and professional reputations.
* **Harassment** of contacts during aggressive loan collection practices.

KingaSec provides an automated, on-device solution to identify and flag these privacy-invasive apps, empowering users to protect their data and reputation.

---

## 🚀 Solution & Key Features

| Feature | Description | Technical Implementation |
| :--- | :--- | :--- |
| **ML-Powered Risk Assessment** | Scans apps and predicts a Privacy Risk Score (0-10) using a custom model. | TensorFlow 2.x, TFLite (24.64 KB), 70-dimensional Feature Vector |
| **Intelligent Scanning** | Extracts all installed apps and categorizes **dangerous permissions** (SMS, Contacts, Location). | Android `PackageManager` API, Kotlin Coroutines |
| **Risk Classification** | Classifies apps into **HIGH**, **MEDIUM**, or **LOW** risk levels based on the predicted score. | Custom logic based on ML output |
| **Privacy-First Design** | All scanning, feature extraction, and ML inference are performed **on-device**. | Zero network dependency for core analysis |

### Model Performance

| Metric | Result |
| :--- | :--- |
| **Model Type** | Custom 4-Layer Neural Network |
| **Model Size** | 24.64 KB (Optimized via TFLite) |
| **R² Score** | **0.8091** (80.91% Accuracy) |
| **Mean Absolute Error (MAE)** | 0.1241 |
| **Training Data** | 366 Kenyan mobile applications dataset |

---

## 🛠️ Technical Stack

### Frontend & Mobile Development
* **Language:** Kotlin
* **Architecture:** MVVM-inspired pattern
* **UI Framework:** Android Jetpack (RecyclerView, CardView), Material Components
* **Async Processing:** Kotlin Coroutines for efficient background scanning
* **Data Handling:** `ViewBinding` for type-safe UI interactions

### Backend & Machine Learning
* **ML Framework:** TensorFlow 2.x
* **Deployment:** TensorFlow Lite (TFLite)
* **Training & Pre-processing:** Python, Pandas, Scikit-learn
* **Key Dependencies:** `org.tensorflow:tensorflow-lite`, `org.tensorflow:tensorflow-lite-support`

---

## ⚙️ App Architecture

### Code Components
| Component | Functionality |
| :--- | :--- |
| `AppScannerService` | Manages the Android `PackageManager` API to collect installed app details and permissions. |
| `MLDataProcessor` | Converts raw application permissions and category data into the required 70-dimensional feature vector for the ML model. |
| `PrivacyMLModel` | Houses the TFLite interpreter and runs the on-device inference using the loaded `privacy_model.tflite` file. |
| `MainActivity` | Orchestrates the scan, displays dashboard metrics, and presents **HIGH-RISK** apps. |
| `AllAppsActivity` | Shows the full list of all scanned apps with sorting and filtering by risk level. |
| `ScanResultsManager` | Singleton object for efficiently sharing scan results data between different activities. |



---

## 📈 Development Journey & Challenges

### 1. Feature Engineering
The primary challenge was converting raw permission data into a format the neural network could understand. This was solved by creating a robust **70-dimensional feature vector** comprised of:
* 1 Scaled numerical feature (Total Permission Count)
* 18 One-Hot Encoded App Categories
* 51 One-Hot Encoded Dangerous Permissions

### 2. Model Size Optimization
The model was optimized and converted to **TensorFlow Lite** with quantization to achieve a minimal size of **24.64 KB**, ensuring a fast load time and minimal app overhead.

### 3. Android API Restrictions
The project required handling modern Android restrictions on package visibility, necessitating the use of the `<queries>` block and justification for the `QUERY_ALL_PACKAGES` permission for comprehensive app scanning.

---

## ✨ Social Impact

KingaSec directly contributes to digital well-being by:
* **Protecting Users:** Shields users from aggressive data harvesting by financial and utility applications.
* **Empowering Decisions:** Provides transparent, data-driven insights, enabling users to make informed decisions about app permissions and installation.
* **Raising Awareness:** Educates the public about the risks associated with excessive app permissions.
