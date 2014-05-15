Bluetooth Test Client
=====================
**Under Development**

*Android application to connect to a PyBluez server and transmit accelrometer and gyroscope data.*

Process
-------

1. Switch on bluetooth with user's permission
	* if user allowed access :
		- start discovering and add found devices to a list
		- on long click device name :
			1. initiate connection to it using ```AsyncTask```
			2. after socket is connected, get read, write streams in a seperate ```AsyncTask```
			3. start streaming sensor data over socket 
	* if user denied access :
		- Notify that we need bluetooth and stop

Data Format
```
1, 4.5, -56.3, 65.2, 12.03, 45.52, 45.34
relative count, ax, ay, az, gx, gy, gz 
```

Frequency is adjustable on client.

Todos
-----
* Trim ```BluetoothWrapper``` class
* <strike>Better User Interface : good enough</strike>
* Run connection checks in background
* Convert ```StreamToDevice``` Thread into ```AsyncTask```
* <strike>Discover devices in background</strike>
* <strike>important : if user denies bluetooth, show a button to restart the process</strike>
* <strike>Initiate connection in a blocking fashion.</strike>


Installation
------------
1. Download or clone this repository and open the folder as a new android studio project
2. *Gradle* sync

Licence
--------
```
The MIT License (MIT)

Copyright (c) [2014] [Shivek Khurana]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```