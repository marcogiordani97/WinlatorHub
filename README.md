# 🎮 Winlator Hub (Legion External Launcher)

**Winlator Hub** è un launcher Android nativo leggero e moderno, sviluppato in Kotlin e Jetpack Compose. È progettato specificamente per tablet ed emulazione handheld (ottimizzato per **Lenovo Legion Y700 Gen 3 / 2025**), consentendo di organizzare, catalogare e lanciare la propria libreria di giochi PC presenti su un SSD esterno USB-C (o pendrive OTG) tramite l'emulatore **Winlator**.

---

## 🚀 Caratteristiche Principali

- 💾 **Scansione SSD via SAF (Storage Access Framework):** Rileva ed esplora l'unità di archiviazione esterna USB-C senza richiedere permessi di root.
- 🔍 **Auto-Detection Eseguibili (.exe):** Scansiona la struttura delle cartelle dell'SSD identificando automaticamente i giochi PC e i relativi file eseguibili.
- ⚡ **Cache Istantanea con Room Database:** Salva l'indice della libreria in un database SQLite locale (`Room`). La scansione avviene solo su richiesta; agli avvii successivi l'app carica la libreria istantaneamente.
- 📱 **Interfaccia Grafica Edge-to-Edge:** Sviluppata al 100% in **Jetpack Compose** (Material Design 3) con supporto orizzontale (landscape) ottimizzato per display da 8.8" a 165Hz.
- 🔌 **Rilevamento Automatico USB OTG:** Riconosce il collegamento del controller di archiviazione USB Mass Storage (es. case UGREEN Dual Protocol).
- 🎯 **Integrazione Winlator:** Avvio rapido con avviso contestuale per l'esecuzione del titolo selezionato dal Drive `D:`.

---

## 🛠️ Stack Tecnologico

- **Linguaggio:** Kotlin
- **UI Framework:** Jetpack Compose (Material3)
- **Architettura:** MVVM (Model-View-ViewModel) + StateFlow
- **Database Locale:** Room (SQLite) + Coroutines
- **Accesso File:** Android Storage Access Framework (`DocumentFile`)
- **Build System:** Gradle (Kotlin DSL `.kts`) + KSP (Kotlin Symbol Processing)

---

## 📁 Struttura del Progetto

```text
com.legion.winlatorhub/
├── data/
│   └── AppDatabase.kt        # Entity (PcGameEntity), DAO (PcGameDao) e Room Database
├── MainActivity.kt           # UI principale Compose, gestione SAF e Intent Winlator
├── MainViewModel.kt          # ViewModel per la scansione asincrona e sincronizzazione Room
res/
└── xml/
    └── device_filter.xml     # Filtro USB OTG per il rilevamento hardware dell'SSD
