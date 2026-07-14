# GeoMed Router - Medical Dispatch and Routing System

> **End-of-Year Project - Ing1 Cy Tech (Software Engineering)**

**GeoMed Router** is a Java-based decision-support application designed to help relieve saturated emergency departments in the **Cergy-Pontoise urban area** (France). Equipped with both a JavaFX graphical user interface and a command-line interface (CLI), the system optimizes the real-time dispatch and routing of incident victims to the most appropriate local healthcare facilities based on live bed availability and medical specialties. 

To achieve this, it merges computational geometry implementing **Delaunay triangulation** and **Voronoi diagrams** to dynamically model hospital coverage areas—with an optimized **A\*; search algorithm** that leverages Voronoi neighbors for spatial filtering, enabling ultra-fast routing over complex regional road networks.

---

## 📸 Application Overview
<img width="1000" height="733" alt="Interactive graphical user interface of the GeoMed Router application displaying a road network map with hospitals as blue nodes, incidents as red nodes, Delaunay triangulation lines, and a green sidebar control panel on the right with various configuration buttons." src="https://github.com/user-attachments/assets/e654dc4b-20b1-4588-99f8-4d31ec7bbe4a" />
---

## Key Features

* **Dual Interface (GUI & CLI)**: Supports an interactive JavaFX graphical client as well as a flexible terminal/CLI mode for batch simulations.
* **Mapping & Road Networks**: Load maps from OpenStreetMap (OSM) or via optimized binary files (`RoadNetwork`, `Edge`).
* **Advanced Algorithmic Engines**:
  * Optimized route calculation using **A\* algorithm** (`RoutingEngine`).
  * Delaunay Triangulation (`DelaunayEngine`) and Voronoi Diagrams (`VoronoiEngine`) to geometrically model hospital coverage areas.
  * Automatic dispatch engine (`DispatchEngine`) based on medical specialties and facility capacities.
* **Data Import (I/O)**: On-the-fly loading of hospitals and incidents via CSV files (`CsvIncidentImporter`, `CsvSiteImporter`).
* **Interactive User Interface**: Dynamic map visualization and lateral control panel (`MapController`, `SidebarController`).
* **Security**: Role-Based Access Control (`UserRole`, `SecurityContext`).
* **Explainability & GDPR Compliance**: Traceability of dispatch decisions (`DispatchDecision`) and generation of GDPR-compliant impact reports (`GDPRReportingService`).

---

## 🏗️ Project Architecture

The source code is organized modularly according to the following packages:

```text
geomed.app
├── algo          # Complex algorithmic logic and geometry (A*, Delaunay, Voronoi)
├── controller    # Controllers for the JavaFX interface (MVC Pattern)
├── explainability # Algorithmic transparency and GDPR logging modules
├── io            # Input/Output management, map serialization, and CSV parsing
├── model         # Domain entities (Hospital, VictimIncident, VoronoiCell, etc.)
├── security      # User roles, permissions, and session management
└── test          # Simulation data generation tools and unit tests
```

---

## 🔐 Security & Ethics Focus

GeoMed Router is designed with security and algorithmic transparency at its core:

### 👥 Role-Based Access Control (RBAC)
The application implements a strict **RBAC system** (`UserRole`, `SecurityContext`) to restrict access to sensitive patient and hospital data. Users must authenticate, and actions (such as declaring incidents, updating hospital bed availability, or viewing general logs) are strictly gated based on their authorized roles (e.g., paramedic, dispatcher, administrator). **To simplify testing and evaluation, you can dynamically switch your active role at any time using the dropdown selector in the control panel.** 

<img width="231" height="170" alt="Role choice (RBAC)" src="https://github.com/user-attachments/assets/c32a8d65-c761-4d8b-be20-39af5943b300" />

### 📊 Algorithmic Transparency & Scoring
To ensure ethical and explainable decision-making, every dispatch decision is backed by an automated **explainability report**. Instead of operating as a "black box," the dispatch algorithm ranks and assigns a detailed score to each available hospital based on several clear criteria:
* **Medical Specialty Match**: Ensures the hospital has the correct medical ward for the victim's emergency.
* **Geographical Distance**: Computes travel time/distance using A* on the road network.
* **Medical History**: Considers critical patient background information to prioritize appropriate care.

<img width="231" height="242" alt="Explainability Report" src="https://github.com/user-attachments/assets/9a094890-c76b-44b7-910e-636af8066604" />

This system guarantees that medical staff can always inspect, understand, and justify why a patient was routed to a specific facility.

---

## 🛠️ Prerequisites and Setup

### Prerequisites
* **Java JDK**: Version 17 or higher.
* **Maven**: Installed and configured in your PATH environment variable.

### Installation
1. Clone the repository to your local machine:
```bash
git clone <REPOSITORY_URL>
cd Projet-Genie-Logiciel
```

2. Compile the project and download dependencies:
```bash
mvn clean install
```

---

## 💻 Running the Project

The application can be executed in two different modes:

### 1. Graphical User Interface (JavaFX GUI)
Launch the interactive graphical application:
```bash
mvn javafx:run
```
* **Interactive Routing**: Simply click on an incident on the map to instantly calculate and visualize the optimized **A\*** route towards the most adequate hospital.

### 2. Command Line Interface (CLI / Terminal)
You can run the application directly inside your terminal to perform automated scenarios:

First, ensure the project is packaged:
```bash
mvn clean package
```

**Option A:** Run with default settings (No custom map)
This will start the CLI with the default built-in map and simulation parameters:
```bash
java -cp target/geomed-router-1.0-SNAPSHOT.jar geomed.app.ConsoleRunner
```

**Option B:** Run with a custom map file
To load a specific optimized binary (`.pglm`) road network, pass the file path as an argument. You can generate a `.pglm` file beforehand by exporting the active map context from the GUI or the CLI:
```bash
java -cp target/geomed-router-1.0-SNAPSHOT.jar geomed.app.ConsoleRunner --map="data/map.pglm"
```


**CLI Features**: 
  * Includes all functionalities of the JavaFX application.
  * Launch random simulation tests with simultaneous dispatch calculation for all incidents at the same time.
  * Batch process routes with the ability to compute optimized itineraries for all loaded incidents simultaneously.


---

## 💡 Quick Start

To quickly test the application after launching it in **GUI mode**:
1. **Load a map**: Go to the `File > Import Map` menu and select a road network (sample data is available in the `data/` folder).
2. **Simulate incidents**: Import a CSV file containing victims via the side panel to watch the `DispatchEngine` automatically allocate patients according to Voronoi cells and available hospital beds.
3. **Trigger routing**: Select and click on any incident icon on the map to compute and draw the optimal path via A\*.

---
