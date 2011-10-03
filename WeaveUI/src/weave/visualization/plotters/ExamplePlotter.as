/*
    Weave (Web-based Analysis and Visualization Environment)
    Copyright (C) 2008-2011 University of Massachusetts Lowell

    This file is a part of Weave.

    Weave is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, Version 3,
    as published by the Free Software Foundation.

    Weave is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Weave.  If not, see <http://www.gnu.org/licenses/>.
*/

package weave.visualization.plotters
{
	import flash.display.BitmapData;
	import flash.display.Graphics;
	import flash.display.Shape;
	import flash.geom.Point;
	
	import mx.controls.Alert;
	
	import weave.Weave;
	import weave.api.data.IAttributeColumn;
	import weave.api.data.IQualifiedKey;
	import weave.api.linkSessionState;
	import weave.api.newLinkableChild;
	import weave.api.primitives.IBounds2D;
	import weave.core.LinkableNumber;
	import weave.data.AttributeColumns.DynamicColumn;
	import weave.data.AttributeColumns.EquationColumn;
	import weave.data.AttributeColumns.FilteredColumn;
	import weave.data.AttributeColumns.SortedColumn;
	import weave.primitives.Bounds2D;
	import weave.utils.BitmapText;
	import weave.utils.ColumnUtils;
	import weave.visualization.plotters.styles.DynamicFillStyle;
	import weave.visualization.plotters.styles.DynamicLineStyle;
	import weave.visualization.plotters.styles.SolidFillStyle;
	import weave.visualization.plotters.styles.SolidLineStyle;
	
	/**
	 * ExamplePlotter
	 * 
	 * @author Curran and Shweta
	 */
	public class ExamplePlotter extends AbstractPlotter
	{
		// reusable point object
		private const point:Point = new Point();
		
		public const data:DynamicColumn = newSpatialProperty(DynamicColumn);
		
		public const lineStyle:SolidLineStyle = registerNonSpatialProperty(new SolidLineStyle());
		public const fillStyle:SolidFillStyle = registerNonSpatialProperty(new SolidFillStyle());
		
		private var radius:Number = 20;
		private var y:Number = 400;
		
		//private static const tempPoint:Point = new Point(); // reusable object, output of projectPoints()
		
			
		public function ExamplePlotter()
		{
			init();
		}
		
		private function init():void
		{
			fillStyle.color.internalDynamicColumn.globalName = Weave.DEFAULT_COLOR_COLUMN;
			setKeySource(data);
			clipDrawing = false;
		}
		
		override protected function addRecordGraphicsToTempShape(recordKey:IQualifiedKey, dataBounds:IBounds2D, screenBounds:IBounds2D, tempShape:Shape):void
		{
			var graphics:Graphics = tempShape.graphics;
			// begin line & fill
			lineStyle.beginLineStyle(recordKey, graphics);				
			fillStyle.beginFillStyle(recordKey, graphics);
			
			point.x = data.getValueFromKey(recordKey, Number);
			point.y = 0;
			dataBounds.projectPointTo(point, screenBounds);
			
			graphics.drawCircle(point.x,point.y,radius);
			graphics.endFill();
		}
		
		
		override public function drawBackground(dataBounds:IBounds2D, screenBounds:IBounds2D, destination:BitmapData):void
		{
			
		}
		
		/**
		 * This gets the data bounds of the bin that a record key falls into.
		 */
		override public function getDataBoundsFromRecordKey(recordKey:IQualifiedKey):Array
		{
			var bounds:IBounds2D = getReusableBounds();
			var dataValue:Number = data.getValueFromKey(recordKey, Number);
			bounds.setCenteredRectangle(dataValue,0,0,0);
			return [bounds];
		}
		
		/**
		 * This function returns a Bounds2D object set to the data bounds associated with the background.
		 * @param outputDataBounds A Bounds2D object to store the result in.
		 */
		override public function getBackgroundDataBounds():IBounds2D
		{
			return getReusableBounds(undefined, -1, undefined, 1);
		}
	}
}
