# Introduction #

For many companies, owning and operating a fleet of vehicles is a major capital expenditure.  Keeping track of vehicles, drivers, and vehicle maintenance requests is a expensive and arduous task. Fleet management hardware and software allow companies to monitor and  log vehicle usage data to find ways to reduce costs and increase efficiency.  Logistics/freight, delivery, car rental, and mobile service companies can all benefit from the ability to analyse, retrieve and store detailed vehicle usage data.  RoadMap uses an Android interface for the system software, and a Raspberry Pi for the vehicle tracking hardware which allows us to create a cost effective solution to fleet management that does away with extremely expensive proprietary hardware and software found in systems currently on the market. 

### What Does RoadMap Do? ###

* Allows a Manager to plan trips based on vehicle availability and location
* Routes are plotted and approved driving zones are set based on trip start and end points
* Facilitates communication between drivers and Managers
* Streamlines vehicle maintenance requests

### System Features ###

* Dual account types (Manager and Driver)
* Instant messaging between Managers and Drivers
* Vehicle maintenance and emergency notifications
* Approved driving zone violation emails
* GPS tracking device that logs vehicle location data to the cloud when the vehicle is operational
* Allows Managers to create routes and approved driving zones with the Google Maps API
* Selectable real-time vehicle information (Origin/Destination, trip status, driver name/ID#, vehicle speed/location)


### Roadmap Hardware ###

* Permanently located inside the vehicle
* Powered by the vehicle's accessory circuit, which ensures the vehicle's location data is logged whenever the vehicle is operational
* Tracks vehicle location via GPS and updates the Firebase Realtime database via WiFi (Protoype tethers off wireless hotspot, future versions would use a GSM chip for cellular data)
* Communicates with the driver's mobile app via Bluetooth to allow access to the system 
* Calculates zone violations and adds them to the database

![What does Roadmap do](graphics/What_does_roadmap_do.jpg)
![Roadmap components](graphics/roadmap_components.jpg)
![Remote Unit](graphics/remote_unit.jpg)
![Manager Perspective](graphics/app_flow_manager_perspective.jpg)
![Driver Perspective](graphics/app_flow_driver_perspective.jpg)
![App UI 1](graphics/app_user_interfaces_1.jpg)
![App UI 2](graphics/app_user_interfaces_2.jpg)
![App UI 3](graphics/app_user_interfaces_3.jpg)
![App UI 4](graphics/app_user_interfaces_4.jpg)
![App UI 5](graphics/app_user_interfaces_5.jpg)
![App UI 6](graphics/app_user_interfaces_6.jpg)
![System Overview](graphics/system_overview.jpg)
