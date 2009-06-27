Welcome to GeoCrawler. This is a J2ME midlet that can sense your position
through GPS or Cell tower (using APIs from opencellid.org) and show you
on a map. I have also added Yahoo! Upcoming integration, so that the app
is a little more useful.

Right now, consider this to be a work in progress. There are a thousand
bugs and glitches to be sorted out. I would be very happy if you point them
out as you use it.

I have tested this on a Nokia N78. It works.

Features:
=========
- GPS sensing
- Cell tower sensing (where known)

- Manual location setting
  - through map scrolling
  - through lat-lon input
  - thorough cell id input (uber-geeky: meant only for developers!)

- Yahoo! Fire Eagle integration
  You can update your location to Yahoo! Fire Eagle location broker
service automatically. See http://fireeagle.yahoo.net for details.

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

- Many configurations to figure out and fiddle with when you are in the 
mood for complex problems to solve.

Bugs:
=====
1. When the manual location is updated, the Fire Eagle is not updated.

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
http://github.com/simonpk/j2me-oauth/tree/master. However, there may
need to be some changes which I did and is not up on Simon's github
yet. If so, please contact me for a patch file.

You will also need the following stuff:
1. JSON parsing library from meapplication-developers available at:
https://meapplicationdevelopers.dev.java.net/svn/meapplicationdevelopers

2. The MathUtil functions not normally provided in J2ME. Courtesy of
akmemobile. The SVN repository is available at:
https://akmemobile.dev.jav.net/svn/akmemobile

License:
========
Frankly, there's nothing to license here. But... since this was developed 
on my company hardware using some company time, there will be a Copyright
and a license soon (following company policy). Till then, consider this to
be a BSD license.
