Version Alpha 0.1
=================

Welcome to GeoCrawler. This is a J2ME midlet that can sense your position
through GPS or Cell tower (using APIs from opencellid.org) and show you
on a map. I have also added Yahoo! Upcoming integration, so that the app
is a little more useful.

Right now, consider this to be a work in progress. There are a thousand
bugs and glitches to be sorted out. I would be very happy if you point them
out as you use it.

I have tested this on a Nokia N78. It works.

Pre-requisites:
===============
If you are planning to use this, please note that your phone needs to
support the following:

1. J2ME (Java Apps) [For geeks: MIDP 2.0, CLDC 1.1]

2. JSR-179 (this is integral to GPS lookup and the app is likely to crash
or not install if this is missing. I am working on making the app function
even if this is not present)

3. Web access (Wireless, GPRS, whatever). There are some issues with the OS
continuously asking for authorization to make a Web access from an app. I
don't know how to turn this off, though I know that phones (like Nokia N95,
have configuration settings that will do a one-time authorization).

4. HTTPS support. This is needed only for the Fire Eagle component right
now. My tests with Blackberry shows that while the above three pre-requisites
are satisfied, this is not. I have to still see if it works with a Blackberry.

Getting it:
===========
You can access the latest JAR from:
http://github.com/anandi/GeoCrawler/raw/master/dist/GeoCrawler.jar
Tiny URL: http://tinyurl.com/n26lpu

Screenshots:
============
Most of the screenshots from the 'screenshots' directory are from an older
version. I did a major UI scrub based on feedback I got for the existing
ones. Although the features are mostly same, the menu items are a little
different. I will fix that soon.

Features:
=========
- GPS sensing
- Cell tower sensing (I think this is not working. Atleast not on N78)
- IP lookup as a fallback. This is a replacement for Cell tower.

- Manual location setting
  - through map scrolling
  - through lat-lon input
  - thorough cell id input (uber-geeky: meant only for developers!)
  - through text strings (requires Fire Eagle authorization)

- Yahoo! Fire Eagle integration
  You can update your location to Yahoo! Fire Eagle location broker
service automatically. See http://fireeagle.yahoo.net for details.
  Also downloads your Fire Eagle location when the app starts up, till
it can detect your location from other beacons like GPS or Cell tower.

- Map integration from Yahoo! maps. The map navigation keys are not
documented in the app (lazy me!)... here they are:
  - '#' : Zoom in
  - '*' : Zoom out
  - Joystick left : Go west
  - Joystick right: Go east
  - Joystick up   : Go north
  - Joystick down : Go south
  - Joystick fire : Set your current location to the center of the map
                    currently displayed.

- Upcoming integration. Automatically background queries the 'best in
place' Upcoming events from http://upcoming.yahoo.com for your current
location. This data is plotted as small icons on the map you see. There
is a 'roaming' mode (see later for keys) where you can scroll through
the icons marked on your display (the default black of the icon changes
to red). The keys are:
  - '#' : Zoom in
  - '*' : Zoom out
  - Joystick left : Previous item
  - Joystick right: Next item
  - Joystick up   : Previous item
  - Joystick down : Next item
  - Joystick fire : See details for the event currently highlighted.

- Changing modes: Use the 'C' key to switch modes. I consider there to
be three modes of operation, though it seems more like 2...
  - Map mode (2 subcases)
    - With your location summary text painted on the map
    - Without your location summary text painted.
  - Roaming mode
  Some keys (like the '#' and the '*' work the same in all modes.

- Many configurations to figure out and fiddle with when you are in the 
mood for complex problems to solve.

Bugs:
=====
  Too many to list here, but it overall works.

Status:
=======
Right now, consider this as Alpha quality. I would be happy if you are
willing to test drive it and give me some feedback at arnob.nandi@yahoo.in.
You will be using my 'test' app on Fire Eagle site, which is not yet
published to gallery. Even if you do not like that, you can still use
GeoCrawler without Fire Eagle integration, though it may lack certain
address resolution capabilities.

Developers:
===========
If you want to use the source, you will also need the OAuth JAR from
http://github.com/fireeagle/j2me-oauth/tree/master.

You will also need the following stuff:
1. JSON parsing library from meapplication-developers available at:
https://meapplicationdevelopers.dev.java.net/svn/meapplicationdevelopers

2. The MathUtil functions not normally provided in J2ME. Courtesy of
akmemobile. The SVN repository is available at:
https://akmemobile.dev.jav.net/svn/akmemobile

Finally, you need to get your own keys. See GeoCrawlerKey.java for the
listing of the keys needed for all functionalities.

License:
========
Frankly, there's nothing to license here. But... since this was developed 
on my company hardware using some company time, there will be a Copyright
and a license soon (following company policy). Till then, consider this to
be a BSD license.
