/*
 * Copyright (c) 2019, Benjamin <https://github.com/genetic-soybean>
 * Copyright (c) 2020, Bram91 <https://github.com/Bram91>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.bram91.brushmarkers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class BrushMarkerMinimapOverlay extends Overlay
{
	private static final int[][] RECT_TRANSFORMS = {
		{ 0, 0 },
		{ 0, Perspective.LOCAL_TILE_SIZE },
		{ Perspective.LOCAL_TILE_SIZE, Perspective.LOCAL_TILE_SIZE },
		{ Perspective.LOCAL_TILE_SIZE, 0 }
	};

	private final Client client;
	private final BrushMarkerConfig config;
	private final BrushMarkerPlugin plugin;

	@Inject
	private BrushMarkerMinimapOverlay(Client client, BrushMarkerConfig config, BrushMarkerPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.drawTileOnMinimmap())
		{
			return null;
		}

		final Collection<ColorTileMarker> points = plugin.getPoints();
		for (final ColorTileMarker point : points)
		{
			WorldPoint worldPoint = point.getWorldPoint();
			if (worldPoint.getPlane() != client.getPlane())
			{
				continue;
			}

			Color tileColor = point.getColor();
			if (tileColor == null)
			{
				// If this is an old tile which has no color, or rememberTileColors is off, use marker color
				tileColor = plugin.getColor();
			}

			drawOnMinimap(graphics, worldPoint, tileColor);
		}

		return null;
	}

	private void drawOnMinimap(Graphics2D graphics, WorldPoint point, Color color)
	{
		if (!point.isInScene(client))
		{
			return;
		}

		int x = (point.getX() - client.getBaseX()) << Perspective.LOCAL_COORD_BITS;
		int y = (point.getY() - client.getBaseY()) << Perspective.LOCAL_COORD_BITS;
		int maxDist = (int) ((20 << Perspective.LOCAL_COORD_BITS) * 4.0 / client.getMinimapZoom());

		Polygon rect = new Polygon();
		for (int[] transform : RECT_TRANSFORMS)
		{
			LocalPoint vertex = new LocalPoint(x + transform[0], y + transform[1]);
			Point mp = Perspective.localToMinimap(client, vertex, maxDist);
			if (mp == null)
			{
				return;
			}
			rect.addPoint(mp.getX(), mp.getY());
		}

		graphics.setColor(color);
		graphics.setStroke(new BasicStroke());
		graphics.drawPolygon(rect);
	}
}
