###
MnoClassifier learns MSISDN-Operator combinations to afterwards predict Operators.
Copyright (C) 2013 MACH Connectivity GmbH

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
###

angular.module('monitoringService', ['ngResource']).factory 'Monitoring', ($resource) ->
	$resource('/api/metrics:history', {},
		get: (method: "GET", params: (history: ""), isArray: true),
		getHistory: (method: "GET", params: (history: "History")))

class MonitoringPush
	websockets = {}
	self = {}

	constructor: (@rootScope) -> self = this

	start: () ->
		create("/wsapi/metrics", this.onMetrics)
		create("/wsapi/metricsHistory", this.onMetricsHistory)

	stop: () ->
		for path, sock of websockets
			do (path, sock) ->
				websockets[path] = null
				sock.close()

	onMetrics: (event) ->

	onMetricsHistory: (event) ->

	create = (path, onMessage) ->
		websockets[path] = new WebSocket("ws://" + window.location.host + path)
		websockets[path].onmessage = (event) ->
			self.rootScope.$apply(() -> onMessage(JSON.parse(event.data)))
		websockets[path].onclose = () ->
			setTimeout((() -> create(path, onMessage) if websockets[path]?), 5000)

angular.module('monitoringPushService', []).factory 'MonitoringPush', ($rootScope) -> new MonitoringPush($rootScope)

window.MonitoringController = ($scope, Monitoring, MonitoringPush) ->
	$scope.metrics = Monitoring.get()

	$scope.metricsHistory = Monitoring.getHistory () -> drawGraphs()

	charts = {}
	drawGraphs = ->
		for name in [
			'java.memory.heap.used'
			'classification.samples'
			'classification.classifications'
			'classification.successRate'
		]
			data = $scope.metricsHistory[name]
			data ?= [date: new Date().getTime(), value: 0]
			charts[name] ?= Morris.Line
				element: name
				data: []
				xkey: 'date',
				ykeys: ['value']
				labels: [name]
				hideHover: 'auto'
			charts[name].setData data

	MonitoringPush.onMetrics = (metrics) -> $scope.metrics = metrics

	MonitoringPush.onMetricsHistory = (history) ->
		$scope.metricsHistory = history
		drawGraphs()

	$scope.$on('$destroy', () -> MonitoringPush.stop())

	MonitoringPush.start()
