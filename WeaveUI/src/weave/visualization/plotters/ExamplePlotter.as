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
		public function ExamplePlotter()
		{
			init();
		}
		
		private function init():void
		{
			var fill:SolidFillStyle = fillStyle.internalObject as SolidFillStyle;
			fill.color.internalDynamicColumn.globalName = Weave.DEFAULT_COLOR_COLUMN;
			
			_beginRadians = new EquationColumn();
			_beginRadians.equation.value = "0.5 * PI + getRunningTotal(spanRadians) - getNumber(spanRadians)";
			_spanRadians = _beginRadians.requestVariable("spanRadians", EquationColumn, true);
			_spanRadians.equation.value = "getNumber(sortedData) / getSum(sortedData) * 2 * PI";
			var sortedData:SortedColumn = _spanRadians.requestVariable("sortedData", SortedColumn, true);
			_filteredData = sortedData.internalDynamicColumn.requestLocalObject(FilteredColumn, true);
			linkSessionState(keySet.keyFilter, _filteredData.filter);
			
			registerSpatialProperty(data);
			setKeySource(_filteredData);
			
			registerNonSpatialProperties(
				Weave.properties.axisFontSize,
				Weave.properties.axisFontColor
			);
		}

		private var _beginRadians:EquationColumn;
		private var _spanRadians:EquationColumn;
		private var _filteredData:FilteredColumn;
		
		public function get data():DynamicColumn { return _filteredData.internalDynamicColumn; }
		public const label:DynamicColumn = newNonSpatialProperty(DynamicColumn);
		
		public const lineStyle:DynamicLineStyle = registerNonSpatialProperty(new DynamicLineStyle(SolidLineStyle));
		public const fillStyle:DynamicFillStyle = registerNonSpatialProperty(new DynamicFillStyle(SolidFillStyle));
		public const labelAngleRatio:LinkableNumber = registerNonSpatialProperty(new LinkableNumber(0, verifyLabelAngleRatio));
		
		private static const tempPoint:Point = new Point(); // reusable object, output of projectPoints()
		
		private function verifyLabelAngleRatio(value:Number):Boolean
		{
			return 0 <= value && value <= 1;
		}
		
		override protected function addRecordGraphicsToTempShape(recordKey:IQualifiedKey, dataBounds:IBounds2D, screenBounds:IBounds2D, tempShape:Shape):void
		{
			// project data coordinates to screen coordinates and draw graphics
			var beginRadians:Number = _beginRadians.getValueFromKey(recordKey, Number);
			var spanRadians:Number = _spanRadians.getValueFromKey(recordKey, Number);
			
			var graphics:Graphics = tempShape.graphics;
			// begin line & fill
			lineStyle.beginLineStyle(recordKey, graphics);				
			fillStyle.beginFillStyle(recordKey, graphics);
			
			var x:Number = Math.random();
			var y:Number = Math.random();
			var radius:Number = 20;
			
			var xMin:Number = dataBounds.getXMin();
			var yMin:Number = dataBounds.getYMin();
			var xMax:Number = dataBounds.getXMax();
			var yMax:Number = dataBounds.getYMax();
			
			//graphics.drawCircle(x,y,radius);
			graphics.drawCircle(400,400,200);
			graphics.drawRect(400,400,300,300);
			graphics.drawRect(xMin,yMin,xMax - xMin, yMax - yMin);
			
			
			graphics.endFill();
		}
		
//		private function drawProjectedWedge(destination:Graphics, dataBounds:IBounds2D, screenBounds:IBounds2D, beginRadians:Number, spanRadians:Number, xDataCenter:Number = 0, yDataCenter:Number = 0, dataRadius:Number = 1):void
//		{
//			tempPoint.x = xDataCenter;
//			tempPoint.y = yDataCenter;
//			dataBounds.projectPointTo(tempPoint, screenBounds);
//			var xScreenCenter:Number = tempPoint.x;
//			var yScreenCenter:Number = tempPoint.y;
//			// convert x,y distance from data coordinates to screen coordinates to get screen radius
//			var xScreenRadius:Number = dataRadius * screenBounds.getWidth() / dataBounds.getWidth() ;
//			var yScreenRadius:Number = dataRadius * screenBounds.getHeight() / dataBounds.getHeight() ;
//			
//			// move to center point
//			destination.moveTo(xScreenCenter, yScreenCenter);
//			// line to beginning of arc, draw arc
//			DrawUtils.arcTo(destination, true, xScreenCenter, yScreenCenter, beginRadians, beginRadians + spanRadians, xScreenRadius, yScreenRadius);
//			// line back to center point
//			destination.lineTo(xScreenCenter, yScreenCenter);
//		}
		
		override public function drawBackground(dataBounds:IBounds2D, screenBounds:IBounds2D, destination:BitmapData):void
		{
			
		}
		
		private const _tempPoint:Point = new Point();
		private const _bitmapText:BitmapText = new BitmapText();
		
		/**
		 * This gets the data bounds of the bin that a record key falls into.
		 */
		override public function getDataBoundsFromRecordKey(recordKey:IQualifiedKey):Array
		{
			var beginRadians:Number = _beginRadians.getValueFromKey(recordKey, Number);
			var spanRadians:Number = _spanRadians.getValueFromKey(recordKey, Number);
			var bounds:IBounds2D = getReusableBounds();
			WedgePlotter.getWedgeBounds(bounds, beginRadians, spanRadians);
			return [bounds];
		}
		
		/**
		 * This function returns a Bounds2D object set to the data bounds associated with the background.
		 * @param outputDataBounds A Bounds2D object to store the result in.
		 */
		override public function getBackgroundDataBounds():IBounds2D
		{
			return getReusableBounds(-1, -1, 1, 1);
		}
	}
}
