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

angular.module('mnoclassifier', [
		'classificationService',
		'trainingService',
		'monitoringService',
		'monitoringPushService',
		'configService'
	]).config ($routeProvider, $locationProvider) ->
		$routeProvider.when('/', templateUrl: "/templates/classification.html")
		$routeProvider.when('/classification', redirectTo: "/")
		$routeProvider.when('/classification/:msisdn', templateUrl: "/templates/classification.html")
		$routeProvider.when('/training', templateUrl: "/templates/training.html")
		$routeProvider.when('/monitoring', templateUrl: "/templates/monitoring.html")
		$routeProvider.when('/configuration', templateUrl: "/templates/config.html")
		$routeProvider.otherwise(redirectTo: "/")

		$locationProvider.html5Mode(true)

window.NavigationController = ($scope, $location) ->
	$scope.menuItems = [
		(name: "Classification", icon: "icon-search", url: "/", regex: "^/(classification\/\\d+)?$"),
		(name: "Training", icon: "icon-wrench", url: "/training", regex: "^/training$"),
		(name: "Monitoring", icon: "icon-dashboard", url: "/monitoring", regex: "^/monitoring$"),
		(name: "Configuration", icon: "icon-cog", url: "/configuration", regex: "^/configuration$")
	].map (menuItem) ->
		menuItem.active = () -> $location.path().match(menuItem.regex)
		menuItem

	$scope.redirect = (path) -> $location.path(path)
