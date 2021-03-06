/* Copyright (C) 2010 MoSync AB

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License, version 2, as published by
the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with this program; see the file COPYING.  If not, write to the Free
Software Foundation, 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.
*/

#include <ma.h>
#include <conprint.h>
#include <MAUtil/Graphics.h>

/*
 Author: mikael.kindborg@mosync.com

 Program that tests screen orientation.

 How to use:

   You should see a green rectangle.

   When the device is rotated the color should change
   to be brighter in landscape orientation and darker
   in portrait orientation.

   Touch screen or press a key to toggle color
   between blue and green.

   Press zero or back to exit.
*/

static int gState;

static void PaintScreen()
{
	MAExtent extent = maGetScrSize();
	int color;
	if (EXTENT_X(extent) > EXTENT_Y(extent)) // Landscape
	{
		color = (gState % 2 == 0 ? 0x00FF00 : 0x0000FF);
	}
	else // Portrait
	{
		color = (gState % 2 == 0 ? 0x009900 : 0x000099);
	}
	maSetColor(color);
	maFillRect(0, 0, EXTENT_X(extent), EXTENT_Y(extent));
	maUpdateScreen();
}

extern "C" int MAMain()
{
	gState = 0;
	PaintScreen();

	while (1)
	{
		MAEvent event;
		maWait(0);
		while(maGetEvent(&event))
		{
			if (event.type == EVENT_TYPE_CLOSE)
			{
				maExit(0);
			}
			else
			if (event.type == EVENT_TYPE_KEY_PRESSED)
			{
				if (event.key == MAK_0 || event.key == MAK_BACK)
				{
					maExit(0);
				}
				else
				{
					++gState;
					PaintScreen();
				}
			}
			else if (event.type == EVENT_TYPE_SCREEN_CHANGED)
			{
				lprintfln("EVENT_TYPE_SCREEN_CHANGED");
				PaintScreen();
			}
			else if (event.type == EVENT_TYPE_POINTER_PRESSED)
			{
				++gState;
				PaintScreen();
			}
		}
	}

	return 0;
}
