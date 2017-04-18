# CustomView
	DrawView
	自定义的画图工具，可以通过修改DrawView中的DrawType属性来修改所绘制的图形
	使用方式：drawView.setDt(DrawType.ROUND); //画圆形
			  drawView.setDt(DrawType.LINE); //画直线
			  drawView.setDt(DrawType.FREE); //涂鸦，即随便画
			  drawView.setDt(DrawType.RECT); //画矩形
			  ...
	通过 setPaintColor(color)；可以设置画笔颜色
		 setPaintStrokeWidth(width);可以设置画笔的宽度